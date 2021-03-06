package com.wanyi.uiframe.dkplayer.fragment;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.dueeeke.videoplayer.player.IjkVideoView;
import com.dueeeke.videoplayer.player.VideoViewManager;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.listener.OnRefreshLoadMoreListener;
import com.wanyi.uiframe.MyApp;
import com.wanyi.uiframe.R;
import com.wanyi.uiframe.aop.impl.AspectVideoWatch;
import com.wanyi.uiframe.api.ApiFacade;
import com.wanyi.uiframe.api.model.dto.vo.IPreMovieVO;
import com.wanyi.uiframe.api.model.dto.vo.IVipVO;
import com.wanyi.uiframe.dkplayer.BaseFragment;
import com.wanyi.uiframe.comment.dialog.CommentDialog;
import com.wanyi.uiframe.dkplayer.adapter.Tiktok3Adapter;
import com.wanyi.uiframe.dkplayer.adapter.Tiktok3DetailAdapter;
import com.wanyi.uiframe.dkplayer.adapter.callback.TikTokCallBack;
import com.wanyi.uiframe.dkplayer.util.Utils;
import com.wanyi.uiframe.dkplayer.util.cache.PreloadManager;
import com.wanyi.uiframe.dkplayer.widget.component.TikTokView;
import com.wanyi.uiframe.eventbus.EIJKPlayEvent;
import com.wanyi.uiframe.eventbus.ENotLoginEvent;
import com.wanyi.uiframe.eventbus.EUserUpgradeEvent;
import com.wanyi.uiframe.mvp.presenter.DynamicMoviePresenter;
import com.wanyi.uiframe.mvp.presenter.LocalMovieWritePresenter;
import com.wanyi.uiframe.mvp.presenter.PlayVipPresenter;
import com.wanyi.uiframe.mvp.presenter.action.IDynamicMovieAction;
import com.wanyi.uiframe.mvp.presenter.callback.ILocalMovieWriteCallback;
import com.wanyi.uiframe.mvp.presenter.callback.IMovieCallback;
import com.wanyi.uiframe.mvp.presenter.callback.IPlayVipViewCallback;
import com.wanyi.uiframe.persistence.entity.HistoryMovieDataDo;
import com.wanyi.uiframe.singleton.EnumPreMovie;
import com.wanyi.uiframe.usercenter.abs.view.INetWorkView;
import com.wanyi.uiframe.usercenter.realize.NetWorkPresenter;
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import es.dmoral.toasty.Toasty;
import io.reactivex.schedulers.Schedulers;

public  class TikTokDetailFullFragment extends BaseFragment implements TikTokCallBack, CommentDialog.CallBack, INetWorkView,
        IMovieCallback, ILocalMovieWriteCallback, IPlayVipViewCallback, OnRefreshLoadMoreListener {


    final String TAG = getClass().getName();

    protected IjkVideoView mIjkVideoView;
    @Override
    protected int getLayoutResId() {
        return R.layout.fragment_tiktok3_detail;
    }

    /**
     * ??????????????????
     */
    private int mCurPos;
    private List<IPreMovieVO> mVideoList = new ArrayList<>();
    private Tiktok3DetailAdapter mTiktok3Adapter;
    private ViewPager2 mViewPager;
    private PreloadManager mPreloadManager;

    /**
     * ???????????????
     */
    CommentDialog commentDialog = new CommentDialog();
    /**
     * VerticalViewPager??????????????????
     */
    private boolean mIsReverseScroll;

    private TikTokView mController;

    private RecyclerView mViewPagerImpl;
    private SmartRefreshLayout smartRefreshLayout;
    /**
     * ?????????????????????
     */
    private NetWorkPresenter netWorkPresenter = new NetWorkPresenter();
    /**
     * ???????????????????????????
     */
    private Boolean netWorkError = false;
    /**
     * ?????????????????????
     */
    LocalMovieWritePresenter movieWritePresenter = new LocalMovieWritePresenter();
    /**
     * ?????????????????????
     */
    private IDynamicMovieAction dynamicMovieAction;
    /**
     * ???????????????vip??????
     */
    private PlayVipPresenter playVipPresenter = new PlayVipPresenter();
    /**
     * ?????????????????????
     */
    private MaterialDialog netTipDialog,userTipDialog,vipTipDialog;



    @Override
    protected void initView() {
        super.initView();
        tiktokVH = null;
        mIjkVideoView = new IjkVideoView(getContext());
        mIjkVideoView.setLooping(true);
        mController = new TikTokView(getContext());
        mIjkVideoView.setVideoController(mController);
        mIjkVideoView.setScreenScale(IjkVideoView.SCREEN_SCALE_CENTER_CROP);
        VideoViewManager.instance().setCurrentVideoPlayer(mIjkVideoView);
        mViewPager = findViewById(R.id.vp2);
        mViewPager.setOffscreenPageLimit(4);
        mTiktok3Adapter = new Tiktok3DetailAdapter(mVideoList);
        mTiktok3Adapter.setCallBack(this);
        mViewPager.setAdapter(mTiktok3Adapter);
        mViewPager.setOverScrollMode(View.OVER_SCROLL_NEVER);
        mViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                mIsReverseScroll = position < mCurPos;
            }

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if(mVideoList.size() != 0) {
                    if(mVideoList.size() - position < 5) {
                        dynamicMovieAction.loadMore();
                    }
                }

                if (position == mCurPos) return;
                mViewPager.post(new Runnable() {
                    @Override
                    public void run() {
                        netWorkPresenter.netCheck();
                        dynamicMovieAction.record(position);
                        startPlay(position);
                    }
                });
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    mPreloadManager.resumePreload(mCurPos, mIsReverseScroll);
                } else {
                    mPreloadManager.pausePreload(mCurPos, mIsReverseScroll);
                }
            }
        });
        //ViewPage2???????????????RecyclerView????????????????????????ViewPager2??????0?????????
        mViewPagerImpl = (RecyclerView) mViewPager.getChildAt(0);
        AspectVideoWatch.hold(getContext());
        smartRefreshLayout = findViewById(R.id.smart_refresh);
        mPreloadManager = PreloadManager.getInstance(getContext());
        netWorkPresenter.attach(this);
        smartRefreshLayout.setOnRefreshLoadMoreListener(this);
        dynamicMovieAction = getDataSource();
        dynamicMovieAction.registerCallBack(this);
        dynamicMovieAction.synData();
        movieWritePresenter.attach(this);
        netWorkPresenter.netCheck();
        playVipPresenter.attach(this);
        initMaterDialog();
        AspectVideoWatch.hold(getContext());
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        movieWritePresenter.detach();
        playVipPresenter.detach();
        mIjkVideoView.release();
        tiktokVH = null;
    }

    /**
     * ??????????????????
     */
    private void initMaterDialog() {
        netTipDialog = new  MaterialDialog.Builder(getContext())
                .content(R.string.network_content)
                .negativeText(R.string.network_pause)
                .positiveText(R.string.network_ignore)
                .onNegative((d,v) -> {
                    // userSelectedPlay = false;
                    NetWorkPresenter.setUserIgnore(false);
                })
                .onPositive((d,v) -> {
                    NetWorkPresenter.setUserIgnore(true);
                    startPlay(mCurPos);
                })
                .build();
        userTipDialog = new MaterialDialog.Builder(getContext())
                .title(R.string.dialog_tip).content(R.string.dialog_load_content)
                .negativeText(R.string.dialog_load_button).onNegative((dialog,which) -> {
                    EventBus.getDefault().post(ENotLoginEvent.NO_LOGIN);
                }).build();
        vipTipDialog = new MaterialDialog.Builder(getContext()).title(R.string.dialog_tip)
                .content(R.string.dialog_upgrade_content).negativeText(R.string.dialog_upgrade_button)
                .onNegative((dialog,which) -> {
                    EventBus.getDefault().post(EUserUpgradeEvent.upgrade);
                })
                .build();
    }

    /**
     * ???????????????????????????????????????????????????
     * @return
     */
    protected IDynamicMovieAction getDataSource() {
        return DynamicMoviePresenter.getInstance();
    }



    private IVipVO getVipVO() {
        IPreMovieVO preMovieVO = mVideoList.get(mCurPos);
        return preMovieVO;
    }


    Tiktok3DetailAdapter.ViewHolder tiktokVH = null;
    /**
     * ????????????????????????VideoView??????
     * @param position
     */
    private void startPlay(int position) {
        // Log.e(getClass().getName(),"userSelectedPlay:" + userSelectedPlay);
        int count = mViewPagerImpl.getChildCount();
        for (int i = 0; i < count; i++) {
            View itemView = mViewPagerImpl.getChildAt(i);
            Tiktok3DetailAdapter.ViewHolder viewHolder = (Tiktok3DetailAdapter.ViewHolder) itemView.getTag();
            tiktokVH = viewHolder;
            if (viewHolder.mPosition == position) {
                mIjkVideoView.release();
                Utils.removeViewFormParent(mIjkVideoView);
                IPreMovieVO tiktokBean = mVideoList.get(position);
                updatePlayNum(tiktokBean.getVideoKey());
                String playUrl = mPreloadManager.getPlayUrl(tiktokBean.getMovieUri());
                Glide.with(this).load(tiktokBean.getPlaceImage()).apply(RequestOptions.noTransformation().placeholder(R.color.white)).into(mController.getThumb());
                mIjkVideoView.setUrl(playUrl);
                viewHolder.mPlayerContainer.addView(mIjkVideoView);
                playVipPresenter.vipPlay(tiktokBean);
                mCurPos = position;
                break;
            }
        }
    }



    /**
     * ??????????????????
     * @param videoKey
     */
    private void updatePlayNum(String videoKey) {
        ApiFacade.createVideo().map(iVideoService -> iVideoService.update_visit(videoKey).blockingFirst())
                .subscribeOn(Schedulers.io())
                .subscribe(videoValueDTO -> {});
    }



    @Override
    protected void reLoad() {
        playVipPresenter.attach(this);
        playVipPresenter.vipPlay(getVipVO());
    }


    @Override
    public void onResume() {
        EventBus.getDefault().register(this);
        super.onResume();
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        mIjkVideoView.pause();
        netWorkPresenter.detach();
        movieWritePresenter.detach();
        playVipPresenter.detach();
        super.onPause();
    }



    public void playFirst() {
        mViewPager.post(() -> {
            netWorkPresenter.netCheck();
            startPlay(0);
        });
    }

    /**
     * ????????????????????????,?????????????????????
     */
    public void saveLoadRecord() {
        IPreMovieVO preMovieVO =  mVideoList.get(mCurPos);
        if(DynamicMoviePresenter.getInstance().getDataSource()!= EnumPreMovie.mycache) {
            HistoryMovieDataDo historyMovieDataDo = new HistoryMovieDataDo();
            historyMovieDataDo.transform(preMovieVO);
            MyApp.getInstance().getDaoSession().getHistoryMovieDataDoDao().save(historyMovieDataDo);
        }
    }





    @Override
    public void callBack(String videoKey) {
        commentDialog.setCallBack(this);
        commentDialog.show(getChildFragmentManager(),commentDialog.getFmTag(),videoKey);
    }

    @Override
    public void downLoad(IPreMovieVO iPreMovieVO) {
        movieWritePresenter.startDown(iPreMovieVO);
    }


    @Override
    public void dialogHide() {
        VideoViewManager.instance().resume();
    }

    @Override
    public void dialogShow() {
        VideoViewManager.instance().pause();
    }

    @Override
    public void noNetWork() {
        netWorkError = true;
        Toasty.Config.getInstance().setTextSize(15).apply();
        Toasty.error(getContext(),R.string.network_error,3*1000,false).show();
    }

    @Override
    public void mobileNetWork() {
        netWorkError = false;
    }

    @Override
    public void wifiNetWork() {
        if(netWorkError) {
            startPlay(mCurPos);
            netWorkError = false;
        }
    }

    @Override
    public void showMessage(String message) {

    }

    @Override
    public void onLoadMore(@NonNull RefreshLayout refreshLayout) {
        dynamicMovieAction.registerCallBack(this);
        dynamicMovieAction.loadMore();
    }

    @Override
    public void onRefresh(@NonNull RefreshLayout refreshLayout) {
        dynamicMovieAction.registerCallBack(this);
        dynamicMovieAction.refresh();
        refreshLayout.finishRefresh(2*1000);
    }

    @Override
    public void onRefresh(List<IPreMovieVO> dataSource) {
        mVideoList.clear();
        mVideoList.addAll(dataSource);
        mTiktok3Adapter.notifyDataSetChanged();
        smartRefreshLayout.finishRefresh();
        mViewPagerImpl.scrollToPosition(0);
        playFirst();
    }

    @Override
    public void onEmpty() {
        Toasty.info(getContext(),R.string.fullscreen_no_data).show();
        smartRefreshLayout.finishRefresh();
    }

    @Override
    public void onLoadMore(List<IPreMovieVO> dataSource) {
        smartRefreshLayout.finishLoadMore();
        int start = mVideoList.size();
        mVideoList.addAll(dataSource);
        mTiktok3Adapter.notifyItemRangeChanged(start,dataSource.size());
    }

    @Override
    public void onComplete() {
        smartRefreshLayout.finishLoadMore();
    }

    @Override
    public void onSyn(List<IPreMovieVO> dataSource, int position) {
        mVideoList.clear();
        mVideoList.addAll(dataSource);
        mTiktok3Adapter.notifyDataSetChanged();
        if(position == 0) {
            playFirst();
        } else {
            netWorkPresenter.netCheck();
            mViewPagerImpl.scrollToPosition(position);
        }

    }

    @Override
    public void movieExist() {
        Toasty.info(getContext(),R.string.fullscreen_video_exist).show();
    }



    @Override
    public void downStart() {
        Toasty.info(getContext(),R.string.fullscreen_video_download).show();
    }

    @Override
    public void noReadPermission() {
        Toasty.warning(getContext(),R.string.fullscreen_refuse_download).show();
    }



    @Override
    public void videoStartPlay() {
        mIjkVideoView.start();
        saveLoadRecord();
    }

    @Override
    public void showLoadingDialog() {
        userTipDialog.show();
    }

    @Override
    public void showBuyCardDialog() {
        vipTipDialog.show();
    }

    @Override
    public void hideDownFunction() {
        if(DynamicMoviePresenter.getInstance().getDataSource()== EnumPreMovie.mycache) {
            return;
        }
        mTiktok3Adapter.setShowDown(false);
        if(tiktokVH != null) {
            tiktokVH.tvDownload.setVisibility(View.GONE);
        }
    }

    @Override
    public void showDownFunction() {
        if(DynamicMoviePresenter.getInstance().getDataSource()== EnumPreMovie.mycache) {
            return;
        }
        mTiktok3Adapter.setShowDown(true);
        if(tiktokVH != null) {
            tiktokVH.tvDownload.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void showErrorTipDialog() {
        //??????????????????
        //Toasty.error(getContext(),"????????????").show();
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void listTikTok(EIJKPlayEvent eijkPlayEvent) {
        switch (eijkPlayEvent) {
            case play:{
                playVipPresenter.vipPlay(getVipVO());
            }
            break;
        }
    }

}
