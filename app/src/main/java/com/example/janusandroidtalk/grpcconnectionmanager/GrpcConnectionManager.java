package com.example.janusandroidtalk.grpcconnectionmanager;

import com.example.janusandroidtalk.tools.AppTools;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import talk_cloud.TalkCloudApp;
import talk_cloud.TalkCloudGrpc;

public class GrpcConnectionManager {
    // TODO Channel for grpc connect
    private ManagedChannel channel;
    // FIXME We chose bloking stub, maybe others better.
    private TalkCloudGrpc.TalkCloudBlockingStub blockingStub;
    // FIXME IM chose new stub, maybe others better
    private TalkCloudGrpc.TalkCloudStub stub;
    // TODO New single thread to handle short-time instant grpc request
    private ExecutorService grpcInstantRequestHandler;
    // TODO Simple schedule simple thread to update online state
    private ScheduledExecutorService onlineStateUpdater;

    private GrpcConnectionManager() {
        channel = ManagedChannelBuilder.forAddress(AppTools.host, AppTools.port).usePlaintext().build();
        blockingStub = TalkCloudGrpc.newBlockingStub(channel);
        stub = TalkCloudGrpc.newStub(channel);

        grpcInstantRequestHandler = Executors.newSingleThreadExecutor();
        onlineStateUpdater = Executors.newScheduledThreadPool(1);
    }

    public TalkCloudGrpc.TalkCloudBlockingStub getBlockingStub() {
        return blockingStub;
    }

    public TalkCloudGrpc.TalkCloudStub getStub() {
        return stub;
    }

    public ExecutorService getGrpcInstantRequestHandler() {
        return grpcInstantRequestHandler;
    }

    public ScheduledExecutorService getOnlineStateUpdater() {
        return onlineStateUpdater;
    }

    private static class SingletonHolder {
        private static GrpcConnectionManager INSTANCE;

        private static void init() {
            INSTANCE = new GrpcConnectionManager();
        }
    }

    public static void initGrpcConnectionManager() {
        SingletonHolder.init();
    }

    public static void closeGrpcConnectionManager() {
        SingletonHolder.INSTANCE.onlineStateUpdater.shutdown();
        SingletonHolder.INSTANCE.grpcInstantRequestHandler.shutdown();
        SingletonHolder.INSTANCE.channel = null;
        SingletonHolder.INSTANCE.blockingStub = null;
    }

    public static GrpcConnectionManager getInstance() {
        return SingletonHolder.INSTANCE;
    }
}

