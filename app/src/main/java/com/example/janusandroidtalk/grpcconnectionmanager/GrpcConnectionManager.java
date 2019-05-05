package com.example.janusandroidtalk.grpcconnectionmanager;

import com.example.janusandroidtalk.bean.UserBean;
import com.example.janusandroidtalk.bean.UserGroupBean;
import com.example.janusandroidtalk.tools.AppTools;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import talk_cloud.TalkCloudApp;
import talk_cloud.TalkCloudGrpc;

public class GrpcConnectionManager {
    // TODO Channel for grpc connect
    private ManagedChannel channel;
    // FIXME We chose bloking stub, maybe others better.
    private TalkCloudGrpc.TalkCloudBlockingStub blockingStub;
    // TODO New single thread to handle short-time instant grpc request
    private ExecutorService grpcInstantRequestHandler;
    // TODO Simple schedule simple thread to update online state
    private ScheduledExecutorService onlineStateUpdater;

    private GrpcConnectionManager() {
        channel = ManagedChannelBuilder.forAddress(AppTools.host, AppTools.port).usePlaintext().build();
        blockingStub = TalkCloudGrpc.newBlockingStub(channel);

        grpcInstantRequestHandler = Executors.newSingleThreadExecutor();
        onlineStateUpdater = Executors.newScheduledThreadPool(1);
    }

    public TalkCloudGrpc.TalkCloudBlockingStub getBlockingStub() {
        return blockingStub;
    }

    public ExecutorService getGrpcInstantRequestHandler() {
        return grpcInstantRequestHandler;
    }

    public ScheduledExecutorService getOnlineStateUpdater() {
        return onlineStateUpdater;
    }

    private static class SingletonHolder {
        private static final GrpcConnectionManager INSTANCE = new GrpcConnectionManager();
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

//    public void updateGroupMembersOnlineState() {
//        System.out.println("AAAAAAAAAAAAAAAAAAAAAAA updateGroupMembersOnlineState again");
//        ArrayList<UserGroupBean> userGroupBeans = UserBean.getUserBean().getUserGroupBeanArrayList();
//
//        for (UserGroupBean userGroupBean : userGroupBeans) {
//            int onlineMembersCount = 0;
//
//            int uid = UserBean.getUserBean().getUserId();
//            int gid = userGroupBean.getUserGroupId();
//            TalkCloudApp.GetGroupInfoReq getGroupInfoReq = TalkCloudApp.GetGroupInfoReq.newBuilder().setUid(uid).setGid(gid).build();
//            TalkCloudApp.GetGroupInfoResp getGroupInfoResp = null;
//            try {
//                Future<TalkCloudApp.GetGroupInfoResp> future = GrpcConnectionManager.getGrpcInstantRequestHandler().submit(new Callable<TalkCloudApp.GetGroupInfoResp>() {
//                    @Override
//                    public TalkCloudApp.GetGroupInfoResp call() throws Exception {
//                        return GrpcConnectionManager.getBlockingStub().getGroupInfo(getGroupInfoReq);
//                    }
//                });
//
//                getGroupInfoResp = future.get();
//            } catch (Exception e) {
//                //TODO Nothing here
//            }
//
//
//            for (TalkCloudApp.UserRecord userRecord : getGroupInfoResp.getGroupInfo().getUsrListList()) {
//                if (userRecord.getOnline() == 2) { // 2 online , 1 offline
//                    onlineMembersCount++;
//                }
//            }
//
//            // Updating global UserBean
//            UserBean.getUserBean().getUserGroupBeanArrayList().get(userGroupBean.getUserGroupId()).setOnlineMembersCount(onlineMembersCount);
//        }
//    }
}

