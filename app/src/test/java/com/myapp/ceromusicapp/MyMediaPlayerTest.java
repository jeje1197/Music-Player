package com.myapp.ceromusicapp;

import org.junit.Assert;
import org.junit.Test;

public class MyMediaPlayerTest {

    @Test
    public void shouldGetMediaPlayer() {
        Assert.assertEquals(MyMediaPlayer.instance, MyMediaPlayer.getInstance());
    }

    @Test
    public void shouldInitializeMediaPlayer() {
        MyMediaPlayer.initializeMediaPlayer();
        Assert.assertNotNull(MyMediaPlayer.instance);
    }

//    @Test
//    void shouldReleaseMediaPlayer() {
//        releaseMediaPlayer();
//    }
}