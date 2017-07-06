package com.google.ads.interactivemedia.v3.samples.videoplayerapp;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.ads.interactivemedia.v3.api.ImaSdkFactory;
import com.google.ads.interactivemedia.v3.samples.samplevideoplayer.SampleVideoPlayer;
import com.google.ads.interactivemedia.v3.samples.samplevideoplayer.SampleVideoPlayer.OnVideoCompletedListener;

import ch.teleboy.ad.AdVideoManager;

/**
 * Main Activity.
 */
public class MyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        orientVideoDescriptionFragment(getResources().getConfiguration().orientation);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        orientVideoDescriptionFragment(configuration.orientation);
    }

    private void orientVideoDescriptionFragment(int orientation) {
        // Hide the extra content when in landscape so the video is as large as possible.
//        FragmentManager fragmentManager = getSupportFragmentManager();
//        Fragment extraContentFragment = fragmentManager.findFragmentById(R.id.videoDescription);
//
//        if (extraContentFragment != null) {
//            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
//            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
//                fragmentTransaction.hide(extraContentFragment);
//            } else {
//                fragmentTransaction.show(extraContentFragment);
//            }
//            fragmentTransaction.commit();
//        }
    }

    /**
     * The main fragment for displaying video content.
     */
    public static class VideoFragment extends Fragment {
        // The video player.
        private SampleVideoPlayer mVideoPlayer;

        // The play button to trigger the ad request.
        private View           mPlayButton;
        private ViewGroup      cont;
        private AdVideoManager adVideoManager;

        @Override
        public void onActivityCreated(Bundle bundle) {
            super.onActivityCreated(bundle);

//            adVideoManager = new AdVideoManager(getContext());
            adVideoManager = new AdVideoManager(ImaSdkFactory.getInstance(), ImaSdkFactory.getInstance().createAdsLoader(getContext()));


            adVideoManager.attachVideoPlayer(mVideoPlayer);
            adVideoManager.attachPlayingViewContainer(cont);
            adVideoManager.addAdVastUrls(getString(R.string.ad_tag_url1), getString(R.string.ad_tag_url2));

            mVideoPlayer.addVideoCompletedListener(new OnVideoCompletedListener() {
                @Override
                public void onVideoCompleted() {
                    adVideoManager.videoComplete();
                }
            });

            // When Play is clicked, request ads and hide the button.
            mPlayButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mVideoPlayer.setVideoPath(getString(R.string.content_url));
                    adVideoManager.requestAd();
                    view.setVisibility(View.GONE);
                }
            });
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_video, container, false);

            mVideoPlayer = (SampleVideoPlayer) rootView.findViewById(R.id.sampleVideoPlayer);
            cont = (ViewGroup) rootView.findViewById(R.id.container);
            mPlayButton = rootView.findViewById(R.id.playButton);

            return rootView;
        }

        @Override
        public void onResume() {

            if (adVideoManager != null && adVideoManager.isAdDisplayed()) {
                adVideoManager.resume();
            } else {
                mVideoPlayer.play();
            }
            super.onResume();
        }

        @Override
        public void onPause() {
            if (adVideoManager != null && adVideoManager.isAdDisplayed()) {
                adVideoManager.pause();
            } else {
                mVideoPlayer.pause();
            }
            super.onPause();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (adVideoManager != null && adVideoManager.isAdDisplayed()) {
                adVideoManager.destroy();
            }
        }
    }

    /**
     * The fragment for displaying any video title or other non-video content.
     */
    public static class VideoDescriptionFragment extends Fragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_video_description, container, false);
        }
    }
}
