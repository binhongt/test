package com.wanyi.uiframe.fragment.container;

import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;
import com.jude.easyrecyclerview.EasyRecyclerView;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.listener.OnRefreshLoadMoreListener;
import com.wanyi.uiframe.R;
import com.wanyi.uiframe.adapter.SearchHistoryAdapter;
import com.wanyi.uiframe.adapter.TendencyHistoryAdapter;
import com.wanyi.uiframe.adapter.callback.ItemClickCallback;
import com.wanyi.uiframe.adapter.callback.RecyclerItemClick;
import com.wanyi.uiframe.api.model.dto.vo.IPreMovieVO;
import com.wanyi.uiframe.api.model.dto.vo.ISearchHistoryVO;
import com.wanyi.uiframe.api.model.dto.vo.ISearchTendencyVO;
import com.wanyi.uiframe.base.BaseFragment;
import com.wanyi.uiframe.dkplayer.activity.tiktok.SearchTikTokActivity;
import com.wanyi.uiframe.dkplayer.adapter.TikTokSearchListAdapter;
import com.wanyi.uiframe.fragment.action.ISearchAction;
import com.wanyi.uiframe.mvp.presenter.SearchHistoryPresenter;
import com.wanyi.uiframe.mvp.presenter.SearchTendencyPresenter;
import com.wanyi.uiframe.mvp.presenter.action.ISearchMovieAction;
import com.wanyi.uiframe.mvp.presenter.action.factory.SearchMovieAction;
import com.wanyi.uiframe.mvp.presenter.callback.IMovieCallback;
import com.wanyi.uiframe.mvp.presenter.callback.ISearchHistoryCallback;
import com.wanyi.uiframe.mvp.presenter.callback.ISearchTendencyCallback;
import com.xuexiang.xaop.annotation.SingleClick;
import com.xuexiang.xpage.annotation.Page;
import com.xuexiang.xui.widget.actionbar.TitleBar;
import com.xuexiang.xui.widget.searchview.MaterialSearchView;
import com.xuexiang.xui.widget.textview.supertextview.SuperTextView;

import net.arvin.itemdecorationhelper.ItemDecorationFactory;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import es.dmoral.toasty.Toasty;

@Page(name = "??????")
public class SearchFragment extends BaseFragment implements ISearchAction, OnRefreshLoadMoreListener, RecyclerItemClick<ISearchHistoryVO>, ISearchHistoryCallback, ISearchTendencyCallback, IMovieCallback
, ItemClickCallback {

    @BindView(R.id.search_view)
    MaterialSearchView searchView;
    @BindView(R.id.history_search)
    RecyclerView historySearch;
    @BindView(R.id.tendency_search)
    RecyclerView tendencySearch;
    @BindView(R.id.video_recycler)
    EasyRecyclerView videoRecycler;
    @BindView(R.id.video_smart_refresh)
    SmartRefreshLayout videoSmartRefresh;
    @BindView(R.id.search_linear)
    LinearLayout searchLinear;
    @BindView(R.id.clear_history)
    SuperTextView clearHistory;

    //??????????????????
    List<IPreMovieVO> searchList = new ArrayList<>();
    List<ISearchHistoryVO> historyList = new ArrayList<>();
    List<ISearchTendencyVO> tendencyList = new ArrayList<>();
    //??????????????????
    TikTokSearchListAdapter tikTokSearchListAdapter = new TikTokSearchListAdapter(searchList);
    //??????????????????
    TendencyHistoryAdapter tendencyHistoryAdapter = new TendencyHistoryAdapter(tendencyList);
    //????????????????????????
    SearchHistoryAdapter searchHistoryAdapter = new SearchHistoryAdapter(historyList);
    //???????????????????????????
    SearchHistoryPresenter searchHistoryPresenter = new SearchHistoryPresenter();
    //???????????????????????????
    SearchTendencyPresenter searchTendencyPresenter = new SearchTendencyPresenter();
    //???????????????????????????
    ISearchMovieAction iSearchMovieAction = SearchMovieAction.getInstance();

    @Override
    protected TitleBar initTitle() {
        TitleBar titleBar = super.initTitle();
        titleBar.addAction(new TitleBar.ImageAction(R.mipmap.ic_action_search_white) {

            @Override
            @SingleClick
            public void performAction(View view) {
                searchView.showSearch();
            }
        });
        return titleBar;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_tab_search;
    }



    @Override
    protected void initViews() {
        tendencyHistoryAdapter.setRecyclerItemClick(this);
        searchHistoryAdapter.setRecyclerItemClick(this);
        videoSmartRefresh.setOnRefreshLoadMoreListener(this);
        searchLinear.setVisibility(View.INVISIBLE);
        searchView.setVoiceSearch(false);
        searchView.setEllipsize(true);
        searchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchHistoryPresenter.saveHistoryVO(query);
                iSearchMovieAction.searchKeyword(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                //Do some magic
                return false;
            }
        });
        searchView.setOnSearchViewListener(new MaterialSearchView.SearchViewListener() {
            @Override
            public void onSearchViewShown() {
                 showSearchUI();
            }

            @Override
            public void onSearchViewClosed() {
                showVideoUI();
            }
        });
        searchView.setSubmitOnClick(true);
        tendencySearch.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        RecyclerView.ItemDecoration itemDecoration = new ItemDecorationFactory.StickyDividerBuilder()
                .dividerColor(getContext().getResources().getColor(R.color.underline_color))
                .dividerHeight(1)
                .build(tendencySearch);
        tendencySearch.addItemDecoration(itemDecoration);
        historySearch.setLayoutManager(getFlexboxLayoutManager());
        //????????????Item???,UI??????
        StaggeredGridLayoutManager manager = new StaggeredGridLayoutManager(2, RecyclerView.VERTICAL) {
            @Override
            public boolean supportsPredictiveItemAnimations() {
                return false;
            }
        };
        videoRecycler.setLayoutManager(manager);
        RecyclerView.ItemDecoration itemDecoration2= new ItemDecorationFactory.DividerBuilder()
                .dividerHeight(6)
                .dividerColor(Color.BLACK)
                .showLastDivider(false)//?????????true
                .build(videoRecycler.getRecyclerView());
        videoRecycler.addItemDecoration(itemDecoration2);
        videoRecycler.setAdapter(tikTokSearchListAdapter);
        tendencySearch.setAdapter(tendencyHistoryAdapter);
        historySearch.setAdapter(searchHistoryAdapter);
        tikTokSearchListAdapter.setClickCallback(this);
        searchHistoryPresenter.attach(this);
        searchTendencyPresenter.attach(this);
        tikTokSearchListAdapter.setEmptyView(LayoutInflater.from(getContext()).inflate(R.layout.include_emptyview,null));
        iSearchMovieAction.registerCallBack(this);
    }


    /**
     * ??????????????????
     */
    public void showSearchUI() {
        searchLinear.setVisibility(View.VISIBLE);
        videoSmartRefresh.setVisibility(View.INVISIBLE);
        searchHistoryPresenter.loadHistoryVo();
        searchTendencyPresenter.loadTendency();
    }

    /**
     * ??????????????????
     */
    public void showVideoUI() {
        searchLinear.setVisibility(View.INVISIBLE);
        videoSmartRefresh.setVisibility(View.VISIBLE);
    }

    /**
     * ???????????????
     *
     * @return
     */
    private FlexboxLayoutManager getFlexboxLayoutManager() {
        //?????????????????????
        FlexboxLayoutManager flexboxLayoutManager = new FlexboxLayoutManager(getContext());
        //flexDirection ?????????????????????????????????????????????????????????????????? LinearLayout ??? vertical ??? horizontal:
        // ??????????????????????????????????????????
        flexboxLayoutManager.setFlexDirection(FlexDirection.ROW);
        //flexWrap ??????????????? Flex ??? LinearLayout ?????????????????????????????????????????????flexWrap??????????????????????????????:
        // ?????????????????????
        flexboxLayoutManager.setFlexWrap(FlexWrap.WRAP);
        //justifyContent ????????????????????????????????????????????????:
        // ????????????????????????
        flexboxLayoutManager.setJustifyContent(JustifyContent.FLEX_START);
        return flexboxLayoutManager;
    }




    @Override
    public void hideClearHistory() {
        clearHistory.setVisibility(View.GONE);
    }

    @Override
    public void showClearHistory() {
        clearHistory.setVisibility(View.VISIBLE);
    }


    @Override
    public void onDestroy() {
        searchHistoryPresenter.detach();
        searchHistoryPresenter = null;
        searchTendencyPresenter.detach();
        searchTendencyPresenter = null;
        super.onDestroy();
    }

    @OnClick(R.id.clear_history)
    public void onClick() {
        searchHistoryPresenter.clearHistoryVO();
    }


    @Override
    public void onLoadMore(@NonNull RefreshLayout refreshLayout) {
        iSearchMovieAction.loadMore();
    }

    @Override
    public void onRefresh(@NonNull RefreshLayout refreshLayout) {
        iSearchMovieAction.refresh();
    }


    @Override
    public void callback(ISearchHistoryVO data) {
        searchHistoryPresenter.saveHistoryVO(data);
        iSearchMovieAction.searchKeyword(data.getTitle());
        searchView.closeSearch();
    }


    @Override
    public void loadHistory(List<ISearchHistoryVO> dataList) {
        historyList.clear();
        historyList.addAll(dataList);
        searchHistoryAdapter.notifyDataSetChanged();
    }

    @Override
    public void hideHistory() {
        historyList.clear();
        searchHistoryAdapter.notifyDataSetChanged();
    }


    @Override
    public void loadTendency(List<ISearchTendencyVO> dataList) {
        this.tendencyList.clear();
        this.tendencyList.addAll(dataList);
        tendencyHistoryAdapter.notifyDataSetChanged();
    }

    @Override
    public void hideTendency() {

    }


    @Override
    public void onRefresh(List<IPreMovieVO> dataSource) {
         searchList.clear();
         searchList.addAll(dataSource);
         tikTokSearchListAdapter.notifyDataSetChanged();
         videoSmartRefresh.finishRefresh();
}

    @Override
    public void onEmpty() {
        searchList.clear();
        tikTokSearchListAdapter.notifyDataSetChanged();
        videoSmartRefresh.finishRefresh();
    }

    @Override
    public void onLoadMore(List<IPreMovieVO> dataSource) {
        searchList.addAll(dataSource);
        tikTokSearchListAdapter.notifyDataSetChanged();
        videoSmartRefresh.finishLoadMore();
    }

    @Override
    public void onComplete() {
        Toasty.info(getContext(),R.string.fullscreen_no_more).show();
        videoSmartRefresh.finishLoadMore();
    }

    @Override
    public void onSyn(List<IPreMovieVO> dataSource, int position) {
        searchList.clear();
        searchList.addAll(dataSource);
        tikTokSearchListAdapter.notifyDataSetChanged();
        videoRecycler.getRecyclerView().scrollToPosition(position);
    }


    @Override
    public void itemClick(Integer position) {
         iSearchMovieAction.record(position);
         Intent intent = new Intent(getActivity(), SearchTikTokActivity.class);
         startActivityForResult(intent,SEARCH_REQUEST_CODE);
    }


    public static Integer SEARCH_REQUEST_CODE = 0x01;
    public static Integer SEARCH_RESULT_CODE = 0x02;

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == SEARCH_REQUEST_CODE && resultCode == SEARCH_RESULT_CODE) {
            iSearchMovieAction.registerCallBack(this);
            iSearchMovieAction.synData();
        }
    }

}
