package me.ihainan.bu.app.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import me.ihainan.bu.app.R;
import me.ihainan.bu.app.models.Post;
import me.ihainan.bu.app.ui.DebugActivity;
import me.ihainan.bu.app.ui.NewPostActivity;
import me.ihainan.bu.app.ui.FullscreenPhotoViewerActivity;
import me.ihainan.bu.app.ui.PostListActivity;
import me.ihainan.bu.app.ui.assist.CustomSpan;
import me.ihainan.bu.app.ui.assist.HtmlTagHandler;
import me.ihainan.bu.app.ui.viewholders.LoadingViewHolder;
import me.ihainan.bu.app.utils.BUApplication;
import me.ihainan.bu.app.utils.CommonUtils;
import me.ihainan.bu.app.utils.network.BUApi;
import me.ihainan.bu.app.utils.ui.PicassoImageGetter;

/**
 * Just for test
 */
public class PostListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final static String TAG = PostListAdapter.class.getSimpleName();
    private final LayoutInflater mLayoutInflater;
    private final String mAuthorName;
    private final long mReplyCount;
    private final Context mContext;
    private final List<Post> mList;

    public PostListAdapter(Context context, List<Post> mList, String authorName, long replyCount) {
        this.mContext = context;
        this.mList = mList;
        mLayoutInflater = LayoutInflater.from(context);
        mAuthorName = authorName;
        mReplyCount = replyCount;
        RecyclerView mRecyclerView = (RecyclerView) ((Activity) mContext).getWindow().getDecorView().findViewById(R.id.detail_recycler_view);
    }

    private final int VIEW_TYPE_ITEM = 0;

    @Override
    public int getItemViewType(int position) {
        int VIEW_TYPE_LOADING = 1;
        return mList.get(position) == null ? VIEW_TYPE_LOADING : VIEW_TYPE_ITEM;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof PostViewHolder) {
            // Do nothing here
            final Post reply = mList.get(position);
            final PostViewHolder viewHolder = (PostViewHolder) holder;

            if (CommonUtils.decode(reply.message).trim().endsWith("</blockquote>")
                    && (reply.attachment == null || reply.attachment.equals(""))) {
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) viewHolder.divider.getLayoutParams();
                params.topMargin = 0;
                viewHolder.divider.setLayoutParams(params);
            } else {
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) viewHolder.divider.getLayoutParams();
                params.topMargin = (int) CommonUtils.convertDpToPixel(10, mContext);
                viewHolder.divider.setLayoutParams(params);
            }

            // 防止附件出现重复
            viewHolder.attachmentLayout.removeAllViews();

            // 楼层
            final long currentLevel = reply.floor;
            viewHolder.number.setText("# " + currentLevel);

            // 头像
            CommonUtils.setUserAvatarClickListener(mContext,
                    viewHolder.avatar, -1,
                    reply.author);
            String avatarURL = reply.avatar == null ? "" : CommonUtils.getRealImageURL(reply.avatar);
            CommonUtils.setAvatarImageView(mContext, viewHolder.avatar,
                    avatarURL, R.drawable.default_avatar);

            // 作者
            if (CommonUtils.decode(reply.author).equals(CommonUtils.decode(mAuthorName))) {
                viewHolder.author.setTextColor(Color.RED);
                viewHolder.author.setText(CommonUtils.decode(reply.author) + "（楼主）");
            } else {
                viewHolder.author.setTextColor(Color.BLACK);
                viewHolder.author.setText(CommonUtils.decode(reply.author));
            }

            // 标题和正文
            if (reply.subject.trim().equals("")) {
                viewHolder.subject.setVisibility(View.GONE);
            } else {
                viewHolder.subject.setVisibility(View.VISIBLE);
            }

            if (reply.message.trim().equals("")) {
                viewHolder.message.setVisibility(View.GONE);
            } else {
                viewHolder.message.setVisibility(View.VISIBLE);
            }

            viewHolder.subject.setText(Html.fromHtml(CommonUtils.addSpaces(CommonUtils.decode(reply.subject))));
            SpannableString spannableString = new SpannableString(
                    Html.fromHtml(
                            CommonUtils.addSpaces(reply.message),
                            new PicassoImageGetter(mContext, viewHolder.message),
                            new HtmlTagHandler()));
            CustomSpan.setUpAllSpans(mContext, spannableString);
            viewHolder.message.setText(spannableString);

            // 日期
            Date datePost = CommonUtils.unixTimeStampToDate(reply.dateline);
            Date dateEdit = CommonUtils.unixTimeStampToDate(Long.valueOf(reply.lastedit));
            String datePostStr = CommonUtils.getRelativeTimeSpanString(datePost);
            if (!(Long.valueOf(reply.lastedit) == 0L) && !datePost.equals(dateEdit)) {
                datePostStr += " (edited at " + DateUtils.formatSameDayTime(dateEdit.getTime(), datePost.getTime(),
                        DateFormat.SHORT, DateFormat.SHORT).toString() + ")";
            }

            // 回复
            viewHolder.reply.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Snackbar.make(mRecyclerView, context.getString(R.string.error_not_implement), Snackbar.LENGTH_LONG).show();
                    Intent intent = new Intent(mContext, NewPostActivity.class);
                    intent.putExtra(NewPostActivity.ACTION_TAG, NewPostActivity.ACTION_NEW_POST);
                    intent.putExtra(NewPostActivity.NEW_POST_TID_TAG, reply.tid);
                    String quoteContent = reply.toQuote();
                    intent.putExtra(NewPostActivity.NEW_POST_QUOTE_CONTENT_TAG, quoteContent);
                    intent.putExtra(NewPostActivity.NEW_POST_MAX_FLOOR_TAG, mReplyCount + 1);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        if (((Activity) mContext).isInMultiWindowMode()) {
                            intent.setFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT |
                                    Intent.FLAG_ACTIVITY_NEW_TASK |
                                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                        }
                    }
                    ((Activity) mContext).startActivityForResult(intent, PostListActivity.REQUEST_NEW_REPLY);
                }
            });

            // 日期
            viewHolder.date.setText(datePostStr);
            if (BUApplication.debugMode) {
                viewHolder.date.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(mContext, DebugActivity.class);
                        intent.putExtra(DebugActivity.ORIGIN_HTML, CommonUtils.decode(reply.originalMessage));
                        intent.putExtra(DebugActivity.NEW_HTML, CommonUtils.decode(reply.message));
                        mContext.startActivity(intent);
                    }
                });
            }

            // 移动端
            if (reply.useMobile) {
                viewHolder.deviceName.setVisibility(View.VISIBLE);
                viewHolder.deviceName.setText(reply.deviceName);
                viewHolder.useMobile.setVisibility(View.VISIBLE);
            } else {
                viewHolder.deviceName.setVisibility(View.INVISIBLE);
                viewHolder.useMobile.setVisibility(View.INVISIBLE);
            }

            // 附件
            if (!(reply.attachment == null || reply.attachment.equals(""))) {
                viewHolder.attachmentLayout.setVisibility(View.VISIBLE);
                showAttachmentView(viewHolder.attachmentLayout, reply);
            } else {
                viewHolder.attachmentLayout.setVisibility(View.GONE);
            }
        } else {
            LoadingViewHolder loadingViewHolder = (LoadingViewHolder) holder;
            loadingViewHolder.progressBar.setIndeterminate(true);
        }
    }

    private void showAttachmentView(LinearLayout linearLayout, final Post reply) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View itemView = inflater.inflate(R.layout.item_thread_detail_attachment, null, false);

        // UI references
        TextView attachmentName = (TextView) itemView.findViewById(R.id.thread_attachment_name);
        RelativeLayout attachmentImageLayout = (RelativeLayout) itemView.findViewById(R.id.thread_attachment_image_layout);
        final TextView clickToLoad = (TextView) itemView.findViewById(R.id.load_image_text);
        final ImageView attachmentImage = (ImageView) itemView.findViewById(R.id.thread_attachment_image);

        // 附件名
        String oriFileName = CommonUtils.decode(reply.filename);

        attachmentName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = BUApi.getBaseURL() + CommonUtils.decode(reply.attachment);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                mContext.startActivity(i);
            }
        });
        attachmentName.setText(oriFileName + "（" + CommonUtils.readableFileSize(reply.filesize) + "）");

        Log.d(TAG, "REPLY >> " + reply.toString());

        // 显示图片
        // String fileType = CommonUtils.decode(reply.filetype);
        if (oriFileName.toLowerCase().endsWith("jpg") || oriFileName.toLowerCase().endsWith("jpeg")
                || oriFileName.toLowerCase().endsWith("gif") || oriFileName.toLowerCase().endsWith("png")) {
            attachmentImageLayout.setVisibility(View.VISIBLE);

            // 尝试从缓存中读取图片，如果缓存中没有图片，则用户点击之后就能加载
            // 图片全部采用 Fit + CenterCrop，大图裁剪，小图拉伸，点击之后自动放大
            final String imageURL = CommonUtils.getRealImageURL(reply.attachment);
            attachmentImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(mContext, FullscreenPhotoViewerActivity.class);
                    intent.putExtra(FullscreenPhotoViewerActivity.IMAGE_URL_TAG, imageURL);
                    mContext.startActivity(intent);
                }
            });

            Picasso.with(mContext)
                    .load(imageURL)
                    .networkPolicy(NetworkPolicy.OFFLINE)
                    .fit()
                    .centerCrop()
                    .into(attachmentImage, new Callback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Picasso >> 缓存图片成功");
                            clickToLoad.setVisibility(View.GONE);
                        }

                        @Override
                        public void onError() {
                            Log.d(TAG, "Picasso >> 缓存图片失败");
                            clickToLoad.setText("点击加载图片");
                            clickToLoad.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Log.d(TAG, "Picasso >> 点击文本，加载图片中");
                                    clickToLoad.setText("正在加载");
                                    Picasso.with(mContext).load(imageURL)
                                            .fit()
                                            .centerCrop()
                                            .into(attachmentImage, new Callback() {
                                                @Override
                                                public void onSuccess() {
                                                    Log.d(TAG, "Picasso >> 加载成功");
                                                    clickToLoad.setVisibility(View.GONE);
                                                }

                                                @Override
                                                public void onError() {
                                                    Log.d(TAG, "Picasso >> 加载 " + imageURL + " 失败");
                                                    clickToLoad.setText("加载失败，点击重试");
                                                }
                                            });
                                }
                            });
                        }
                    });
        } else {
            attachmentImageLayout.setVisibility(View.GONE);
        }

        linearLayout.addView(itemView);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_ITEM) {
            view = mLayoutInflater.inflate(R.layout.item_thread_detail_new, parent, false);
            return new PostViewHolder(view, mContext);
        } else {
            view = mLayoutInflater.inflate(R.layout.listview_progress_bar, parent, false);
            return new LoadingViewHolder(view);
        }
    }

    @Override
    public int getItemCount() {
        return mList == null ? 0 : mList.size();
    }

    public class PostViewHolder extends RecyclerView.ViewHolder {
        public final ImageView avatar;
        public final ImageView reply;
        public final ImageView useMobile;
        public final TextView author;
        public final TextView subject;
        public final TextView date;
        public final TextView number;
        public final TextView message;
        public final TextView deviceName;
        public final LinearLayout attachmentLayout;
        public LinearLayout rootLayout;
        public View divider;

        public PostViewHolder(View itemView, final Context context) {
            super(itemView);

            avatar = (ImageView) itemView.findViewById(R.id.thread_author_avatar);
            author = (TextView) itemView.findViewById(R.id.thread_author_name);
            subject = (TextView) itemView.findViewById(R.id.thread_subject);
            date = (TextView) itemView.findViewById(R.id.post_date);
            number = (TextView) itemView.
                    findViewById(R.id.post_floor);
            reply = (ImageView) itemView.findViewById(R.id.btn_repost);

            subject.setTextSize(TypedValue.COMPLEX_UNIT_SP, BUApplication.titleFontSize);

            message = (TextView) itemView.findViewById(R.id.thread_message);
            message.setMovementMethod(new CustomSpan.LinkTouchMovementMethod());
            message.setTextSize(TypedValue.COMPLEX_UNIT_SP, BUApplication.fontSize);
            message.setLineSpacing(BUApplication.lineSpacingExtra, BUApplication.lineSpacingMultiplier);

            useMobile = (ImageView) itemView.findViewById(R.id.thread_from_mobile);
            deviceName = (TextView) itemView.findViewById(R.id.device_name);

            divider = itemView.findViewById(R.id.divider);
            attachmentLayout = (LinearLayout) itemView.findViewById(R.id.thread_attachment_layout);
        }
    }
}
