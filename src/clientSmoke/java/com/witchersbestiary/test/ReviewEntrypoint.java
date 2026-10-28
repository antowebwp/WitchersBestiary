package com.witchersbestiary.test;
public final class ReviewEntrypoint implements net.fabricmc.api.ClientModInitializer {public void onInitializeClient(){if(Boolean.getBoolean("witchersbestiary.swampReview"))new SwampLordReviewClient().onInitializeClient();else new NoticeBoardReviewClient().onInitializeClient();}}
