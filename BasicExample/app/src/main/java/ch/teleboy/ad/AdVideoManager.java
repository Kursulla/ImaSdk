package ch.teleboy.ad;

import android.util.Log;
import android.view.ViewGroup;

import com.google.ads.interactivemedia.v3.api.AdDisplayContainer;
import com.google.ads.interactivemedia.v3.api.AdErrorEvent;
import com.google.ads.interactivemedia.v3.api.AdEvent;
import com.google.ads.interactivemedia.v3.api.AdsLoader;
import com.google.ads.interactivemedia.v3.api.AdsManager;
import com.google.ads.interactivemedia.v3.api.AdsManagerLoadedEvent;
import com.google.ads.interactivemedia.v3.api.AdsRequest;
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory;
import com.google.ads.interactivemedia.v3.api.player.ContentProgressProvider;
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@SuppressWarnings("WeakerAccess")
public class AdVideoManager {
    private static final String TAG = "MyAdManager";
    private ImaSdkFactory      sdkFactory;
    private AdsLoader          adsLoader;// The AdsLoader instance exposes the requestAds method.
    private AdsManager         adsManager;// AdsManager exposes methods to control ad playback and listen to ad events.
    private ContentVideoPlayer contentVideoPlayer;
    private ViewGroup          playingContainer;
    private boolean            isAdDisplayed; // Whether an ad is displayed.
    private List<String>       adVastUrls;
    private int adNumToBePlayed = 0;
    private AdDisplayContainer adDisplayContainer;


    public AdVideoManager(ImaSdkFactory sdkFactory, AdsLoader adsLoader) {
        this.adVastUrls = new ArrayList<>();
        this.sdkFactory = sdkFactory;
        this.adsLoader = adsLoader;
        this.adsLoader.addAdErrorListener(new AdErrorHandling());
        this.adsLoader.addAdsLoadedListener(new AdsLoader.AdsLoadedListener() {
            @Override
            public void onAdsManagerLoaded(AdsManagerLoadedEvent adsManagerLoadedEvent) {
                // Ads were successfully loaded, so get the AdsManager instance. AdsManager has events for ad playback and errors.
                adsManager = adsManagerLoadedEvent.getAdsManager();
                adsManager.addAdErrorListener(new AdErrorHandling());
                adsManager.addAdEventListener(new AdEventsListener());
                adsManager.init();
            }
        });
    }

    public void addAdVastUrls(String... urls) {
        adVastUrls = Arrays.asList(urls);
    }

    /**
     * Handle completed event for playing post-rolls.
     * Has to be called when AdVideoPlayer finishes playing.
     */
    public void videoComplete() {
        if (adsLoader != null) {
            adsLoader.contentComplete();
        }
    }

    /**
     * If you need to sync ad playing with real content playing, we need that player. It has to implement {@link ContentVideoPlayer}
     *
     * @param videoPlayer implementation of a {@link ContentVideoPlayer}
     */
    public void attachVideoPlayer(ContentVideoPlayer videoPlayer) {
        this.contentVideoPlayer = videoPlayer;
    }

    /**
     * You have to attach some {@link ViewGroup} if you want to have video ad player somewhere!
     *
     * @param playingContainer Any Android {@link ViewGroup} implementation.
     */
    public void attachPlayingViewContainer(ViewGroup playingContainer) {
        this.playingContainer = playingContainer;
    }

    /**
     * For handling Lifecycle states.
     */
    public void resume() {
        if (adsManager != null) {
            adsManager.resume();
        }
    }

    /**
     * For handling Lifecycle states.
     */
    public void pause() {
        if (adsManager != null) {
            adsManager.pause();
        }
    }

    public void destroy() {
        if (adsManager != null) {
            adsManager.destroy();
        }
    }

    /**
     * Should be called when you are ready to play ad.
     */
    public void requestAd() {
        if (playingContainer == null) {
            throw new IllegalStateException("You have to attach ViewGroup first! Video ad has to be drawn somewhere");
        }
        if (contentVideoPlayer == null) {
            throw new IllegalStateException("You have to attach AdVideoPlayer first! Video ad has to be played somehow");
        }
        if (adVastUrls.isEmpty()) {
            contentVideoPlayer.play();
            return;
        }
        performRequest();
    }

    public boolean isAdDisplayed() {
        return isAdDisplayed;
    }


    private void performRequest() {
        if(!hasMoreAdsToPlay()){
            return;
        }
        if (adDisplayContainer == null) {
            adDisplayContainer = sdkFactory.createAdDisplayContainer();
            adDisplayContainer.setAdContainer(playingContainer);
        }
        // Create the ads request.
        AdsRequest request = sdkFactory.createAdsRequest();
        request.setAdTagUrl(getAdUrlToRequest());
        request.setAdDisplayContainer(adDisplayContainer);
        request.setContentProgressProvider(new ContentProgressProvider() {
            @Override
            public VideoProgressUpdate getContentProgress() {
                if (isAdDisplayed || contentVideoPlayer == null || contentVideoPlayer.getDuration() <= 0) {
                    return VideoProgressUpdate.VIDEO_TIME_NOT_READY;
                }
                return new VideoProgressUpdate(contentVideoPlayer.getCurrentPosition(), contentVideoPlayer.getDuration());
            }
        });

        // Request the ad and after the ad is loaded, onAdsManagerLoaded() will be called.
        adsLoader.requestAds(request);
    }

    /**
     * Process error how you want! You can play content, or proceed to the next ad or retry.......
     */
    public class AdErrorHandling implements AdErrorEvent.AdErrorListener {
        @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
        @Override
        public void onAdError(AdErrorEvent adErrorEvent) {//todo notify GrayLog what happened and move on!
            Log.e(TAG, "AdsManager Ad Error: " + adErrorEvent.getError().getMessage());
            adErrorEvent.getError().printStackTrace();
            contentVideoPlayer.play();
        }
    }

    class AdEventsListener implements AdEvent.AdEventListener {
        @Override
        public void onAdEvent(AdEvent adEvent) {
            Log.i(TAG, "Event: " + adEvent.getType());

            // These are the suggested event types to handle. For full list of all ad event
            // types, see the documentation for AdEvent.AdEventType.
            switch (adEvent.getType()) {
                case LOADED:
                    // AdEventType.LOADED will be fired when ads are ready to be played.
                    // AdsManager.start() begins ad playback. This method is ignored for VMAP or
                    // ad rules playlists, as the SDK will automatically start executing the
                    // playlist.
                    incrementNumOfPlayedAd();
                    adsManager.start();
                    break;
                case CONTENT_PAUSE_REQUESTED:
                    // AdEventType.CONTENT_PAUSE_REQUESTED is fired immediately before a video
                    // ad is played.
                    isAdDisplayed = true;
                    contentVideoPlayer.pause();
                    break;
                case CONTENT_RESUME_REQUESTED:
                    // AdEventType.CONTENT_RESUME_REQUESTED is fired when the ad is completed
                    // and you should start playing your content.
                    if (!hasMoreAdsToPlay()) {
                        isAdDisplayed = false;
                        contentVideoPlayer.play();
                    }
                    break;
                case ALL_ADS_COMPLETED:
                    if (adsManager != null) {
                        adsManager.destroy();
                        adsManager = null;
                    }
                    performRequest();
                    break;
                default:
                    break;
            }
        }
    }


    void incrementNumOfPlayedAd() {
        adNumToBePlayed++;
    }

    private boolean hasMoreAdsToPlay() {
        return adNumToBePlayed < adVastUrls.size();
    }

    private String getAdUrlToRequest() {
        return adVastUrls.get(adNumToBePlayed);
    }
}
