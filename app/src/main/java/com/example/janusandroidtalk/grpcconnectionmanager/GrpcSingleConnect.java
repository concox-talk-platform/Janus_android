package com.example.janusandroidtalk.grpcconnectionmanager;

import com.example.janusandroidtalk.tools.AppTools;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import talk_cloud.TalkCloudGrpc;

public class GrpcSingleConnect {
    private final static GrpcSingleConnect instance = new GrpcSingleConnect();
    private ManagedChannel channel = null;
    private TalkCloudGrpc.TalkCloudBlockingStub blockingStub = null;

    private GrpcSingleConnect() {
        channel = ManagedChannelBuilder.forAddress(AppTools.host, AppTools.port).usePlaintext().build();
        blockingStub = TalkCloudGrpc. newBlockingStub(channel);
    }

    public static GrpcSingleConnect getGrpcConnect() {
        return instance;
    }

    public ManagedChannel getChannel() {
        return channel;
    }

    public TalkCloudGrpc.TalkCloudBlockingStub getBlockingStub() {
        return blockingStub;
    }

    //TODO Unique thread handles different requests by FIFO
    public static ExecutorService executor = Executors.newSingleThreadExecutor();
}
