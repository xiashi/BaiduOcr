package com.example.baidu_ocr;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.listener.OnItemClickListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GridImageAdapter extends RecyclerView.Adapter<GridImageAdapter.ViewHolder> {
    public static final String TAG = "----GridImageAdapter";
    public static final int TYPE_CAMERA = 1;
    public static final int TYPE_PICTURE = 2;
    private List<LocalMedia> list = new ArrayList<>();
    private int selectMax = 100;
    private final LayoutInflater mInflater;

    public void setUploadStatus(int index, int status) {
        if (list != null) {
            list.get(index).setOrientation(status);
        }
        notifyDataSetChanged();

    }

    public GridImageAdapter(Context context, onAddPicClickListener mOnAddPicClickListener) {
        this.mInflater = LayoutInflater.from(context);
        this.mOnAddPicClickListener = mOnAddPicClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = mInflater.inflate(R.layout.adapter_filter_image, viewGroup, false);
        return new ViewHolder(view);
    }

    /**
     * 设置值
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        //少于MaxSize张，显示继续添加的图标
        if (getItemViewType(position) == TYPE_CAMERA) {
            viewHolder.mImg.setImageResource(R.drawable.ic_add_image);
            viewHolder.mImg.setOnClickListener(v -> mOnAddPicClickListener.onAddPicClick());
        } else {
            LocalMedia media = list.get(position);



            int chooseModel = media.getChooseModel();
            String path;
            if (media.isCut() && !media.isCompressed()) {
                // 裁剪过
                path = media.getCutPath();
            } else if (media.isCut() || media.isCompressed()) {
                // 压缩过,或者裁剪同时压缩过,以最终压缩过图片为准
                path = media.getCompressPath();
            } else {
                // 原图
                path = media.getPath();
            }

            Log.i(TAG, "原图地址::" + media.getPath());

            if (media.isCut()) {
                Log.i(TAG, "裁剪地址::" + media.getCutPath());
            }
            if (media.isCompressed()) {
                Log.i(TAG, "压缩地址::" + media.getCompressPath());
                Log.i(TAG, "压缩后文件大小::" + new File(media.getCompressPath()).length() / 1024 + "k");
            }
            if (!TextUtils.isEmpty(media.getAndroidQToPath())) {
                Log.i(TAG, "Android Q特有地址::" + media.getAndroidQToPath());
            }
            if (media.isOriginal()) {
                Log.i(TAG, "是否开启原图功能::" + true);
                Log.i(TAG, "开启原图功能后地址::" + media.getOriginalPath());
            }

            long duration = media.getDuration();

            if (chooseModel == PictureMimeType.ofAudio()) {
                viewHolder.mImg.setImageResource(R.drawable.picture_audio_placeholder);
            } else {
                Glide.with(viewHolder.itemView.getContext())
                        .load(PictureMimeType.isContent(path) && !media.isCut() && !media.isCompressed() ? Uri.parse(path)
                                : path)
                        .centerCrop()
                        .placeholder(R.color.app_color_f6)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(viewHolder.mImg);
            }
            //itemView 的点击事件
            if (mItemClickListener != null) {
                viewHolder.itemView.setOnClickListener(v -> {
                    int adapterPosition = viewHolder.getAbsoluteAdapterPosition();
                    mItemClickListener.onItemClick(v, adapterPosition);
                });
            }


        }

    }

    @Override
    public int getItemCount() {
        if (list.size() < selectMax) {
            return list.size() + 1;
        } else {
            return list.size();
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (isShowAddItem(position)) {
            return TYPE_CAMERA;
        } else {
            return TYPE_PICTURE;
        }
    }

    private boolean isShowAddItem(int position) {
        int size = list.size();
        return position == size;
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView mImg;


        public ViewHolder(View view) {
            super(view);
            mImg = view.findViewById(R.id.fiv);
        }
    }

    /**
     * 删除
     */
    public void delete(int position) {
        try {
            if (position != RecyclerView.NO_POSITION && list.size() > position) {
                list.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, list.size());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setSelectMax(int selectMax) {
        this.selectMax = selectMax;
    }

    public void setList(List<LocalMedia> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    public List<LocalMedia> getData() {
        return list == null ? new ArrayList<>() : list;
    }

    public void remove(int position) {
        if (list != null && position < list.size()) {
            list.remove(position);
        }
    }

    /**
     * 点击添加图片跳转
     */
    private final onAddPicClickListener mOnAddPicClickListener;

    public interface onAddPicClickListener {
        void onAddPicClick();
    }


    private OnItemClickListener mItemClickListener;

    public void setOnItemClickListener(OnItemClickListener l) {
        this.mItemClickListener = l;
    }



}
