package com.wanyi.uiframe.comment.dialog;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.wanyi.uiframe.R;
import com.wanyi.uiframe.aop.AopUserLogin;
import com.wanyi.uiframe.api.callback.ApiVideo;
import com.wanyi.uiframe.comment.CommitAdapter;
import com.wanyi.uiframe.comment.SoftKeyBoardListener;
import com.wanyi.uiframe.comment.SoftKeyHideShow;
import com.wanyi.uiframe.comment.action.ICommentItem;
import com.wanyi.uiframe.dialog.BaseDialogFragment;
import com.wanyi.uiframe.eventbus.ECommentUpdate;
import com.xuexiang.xui.widget.statelayout.SimpleAnimationListener;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;
import butterknife.BindView;

public class CommentDialog extends BaseDialogFragment {

    @BindView(R.id.recyclerViewCommit)
    RecyclerView recyclerViewCommit;
    @BindView(R.id.tv_context)
    TextView tv_context;
    @BindView(R.id.tv_send)
    TextView tv_send;
    @BindView(R.id.rl_bottom)
    RelativeLayout rl_bottom;
    @BindView(R.id.commit)
    View commit;
    @BindView(R.id.tv_shape)
    TextView tv_shape;
    @BindView(R.id.tv_shape2)
    TextView tv_shape2;
    @BindView(R.id.ll_cancel)
    LinearLayout ll_cancel;
    @BindView(R.id.et_context)
    EditText et_context;
    @BindView(R.id.all_comment)
    TextView all_comment;
    //dialog?????????
    CallBack callBack;

    /**
     * ????????????
     */
    String videoKey;

    private SoftKeyBoardListener softKeyBoardListener;//???????????????

    private CommitAdapter commitAdapter;

    List<ICommentItem> dataList = new ArrayList<>();

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_comment;
    }

    @Override
    protected boolean isTouchHide() {
        return true;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setSoftKeyBoardListener();
        showCommitDialog();
        callBack.dialogShow();
    }

    public void setCallBack(CallBack callBack) {
        this.callBack = callBack;
    }

    public void show(FragmentManager manager, String tag, String videoKey) {
        super.show(manager, tag);
        this.videoKey = videoKey;
    }

    /**
     * ????????????
     */
    public void showCommitDialog() {
        commitAdapter = new CommitAdapter(getContext(),dataList);
        recyclerViewCommit.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewCommit.setAdapter(commitAdapter);
        ApiVideo.showCommentData(videoKey, source -> {
            dataList.clear();
            dataList.addAll(source);
            all_comment.setText(String.format("????????????(%d)",source.size()));
            commitAdapter.notifyDataSetChanged();
        });
        //??????????????????????????????
        Animation showAction = AnimationUtils.loadAnimation(getContext(), R.anim.actionsheet_dialog_in);
        commit.startAnimation(showAction);

        //?????????????????????
        commit.setVisibility(View.VISIBLE);
        tv_shape.setVisibility(View.VISIBLE);
        //????????????
        ll_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideCommit();
                tv_shape.setVisibility(View.GONE);
            }
        });
        //????????????,???????????????????????????
        tv_shape.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideCommit();
            }
        });
        //??????????????????
        tv_context.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SoftKeyHideShow.HideShowSoftKey(getContext());//???????????????
            }
        });
        //?????????????????????????????????????????????????????????
        tv_shape2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SoftKeyHideShow.HideShowSoftKey(getContext());//???????????????
            }
        });
        //????????????
        tv_send.setOnClickListener(new View.OnClickListener() {

            @AopUserLogin
            @Override
            public void onClick(View view) {
                String comment = et_context.getText().toString().trim();
                et_context.setText("");
                ApiVideo.showReplayStatus(videoKey,comment, isRight -> {
                    if(isRight) {
                        ApiVideo.showCommentData(videoKey, source -> {
                            dataList.clear();
                            dataList.addAll(source);
                            all_comment.setText(String.format("????????????(%d)",source.size()));
                            commitAdapter.notifyDataSetChanged();
                        });
                        ECommentUpdate eCommentUpdate = new ECommentUpdate();
                        eCommentUpdate.setVideoKey(videoKey);
                        EventBus.getDefault().post(eCommentUpdate);
                    }
                });
                SoftKeyHideShow.HideShowSoftKey(getContext());//???????????????
            }
        });
    }



    //????????????
    private void hideCommit() {
        Animation hideAction = AnimationUtils.loadAnimation(getContext(),R.anim.actionsheet_dialog_out);
        hideAction.setAnimationListener(new SimpleAnimationListener(){
            @Override
            public void onAnimationEnd(Animation animation) {
                dismiss();
                super.onAnimationEnd(animation);
            }
        });
        rootView.startAnimation(hideAction);
    }

    /**
     * ???????????????
     */
    private void setSoftKeyBoardListener() {
        softKeyBoardListener = new SoftKeyBoardListener(getActivity());
        //?????????????????????
        softKeyBoardListener.setListener(new SoftKeyBoardListener.OnSoftKeyBoardChangeListener() {
            @Override
            public void keyBoardShow(int height) {
                //????????????????????????
                ViewGroup.LayoutParams params = rl_bottom.getLayoutParams();
                rl_bottom.setPadding(0, 0, 0, height);
                rl_bottom.setLayoutParams(params);
                //???????????????????????????
                rl_bottom.setVisibility(View.VISIBLE);
                tv_shape2.setVisibility(View.VISIBLE);

                et_context.setFocusable(true);
                et_context.setFocusableInTouchMode(true);
                et_context.setCursorVisible(true);
                et_context.requestFocus();
            }

            @Override
            public void keyBoardHide(int height) {
                //???????????????????????????
                rl_bottom.setVisibility(View.GONE);
                tv_shape2.setVisibility(View.GONE);
                et_context.setFocusable(false);
                et_context.setFocusableInTouchMode(false);
                et_context.setCursorVisible(false);
            }
        });
        //??????????????????,???????????????
        et_context.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
            @Override
            public void onClick(View view) {
                InputMethodManager manager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                manager.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
            }
        });
        //??????EditText??????????????????????????????
        et_context.setOnTouchListener(new View.OnTouchListener() {
            //????????????????????????
            int flag = 0;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                flag++;
                if (flag == 2) {
                    flag = 0;//?????????????????????
                    //????????????
                    et_context.setFocusable(true);
                    et_context.setFocusableInTouchMode(true);
                    et_context.setCursorVisible(true);
                }
                return false;
            }
        });
    }

    @Override
    public void dismiss() {
        dataList.clear();
        callBack.dialogHide();
        super.dismiss();
    }


    public interface CallBack {

        /**
         * ???????????????
         */
        void dialogHide();

        /**
         * ???????????????
         */
        void dialogShow();

    }



}
