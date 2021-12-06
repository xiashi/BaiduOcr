package com.example.baidu_ocr;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.ocr.sdk.OCR;
import com.baidu.ocr.sdk.OnResultListener;
import com.baidu.ocr.sdk.exception.OCRError;
import com.baidu.ocr.sdk.model.AccessToken;
import com.example.baidu_ocr.manager.FullyGridLayoutManager;
import com.google.gson.Gson;
import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.animators.AnimationType;
import com.luck.picture.lib.app.PictureAppMaster;
import com.luck.picture.lib.camera.CustomCameraView;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.decoration.GridSpacingItemDecoration;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.entity.MediaExtraInfo;
import com.luck.picture.lib.listener.OnResultCallbackListener;
import com.luck.picture.lib.tools.MediaUtils;
import com.luck.picture.lib.tools.ScreenUtils;
import com.luck.picture.lib.tools.SdkVersionUtils;
import com.yalantis.ucrop.view.OverlayView;

import java.lang.ref.WeakReference;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static String TAG = "---PictureSelectorTag---";
    private AlertDialog.Builder alertDialog;
    private boolean hasGotToken = false;
    RecyclerView mRecyclerView;
    TextView imgText;
    private GridImageAdapter mAdapter;
    private int maxSelectNum = 100;//最大加载图片
    private ActivityResultLauncher<Intent> launcherResult;
    private List<LocalMedia> selectList;
    private DialogLoad dialogLoad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dialogLoad = new DialogLoad(this);
        alertDialog = new AlertDialog.Builder(this);
        mRecyclerView = findViewById(R.id.rv_sign_pic);
        imgText = findViewById(R.id.tv_img);

        FullyGridLayoutManager manager = new FullyGridLayoutManager(this,
                4, GridLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(manager);
        mRecyclerView.addItemDecoration(new GridSpacingItemDecoration(4,
                ScreenUtils.dip2px(this, 8), false));
        mAdapter = new GridImageAdapter(this, onAddPicClickListener);
        mAdapter.setSelectMax(maxSelectNum);
        mRecyclerView.setAdapter(mAdapter);

        // 通用文字识别
        findViewById(R.id.general_basic_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!checkTokenStatus()) {
                    return;
                }
                String imagePath = getImagePath(0);
                if (imagePath != null)
                    RecognizeService.recGeneralBasic(MainActivity.this, imagePath,
                            new RecognizeService.ServiceListener() {
                                @Override
                                public void onResult(String result) {
                                    infoPopText(result);
                                }
                            });
            }
        });

        // 通用文字识别(高精度版)
        findViewById(R.id.accurate_basic_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!checkTokenStatus()) {
                    return;
                }
                if (selectList != null && selectList.size() > 0) {
                    mSize = selectList.size();
                    ProgressUtils.showProgressDialog(dialogLoad);
                    getImageText(0);

                }
            }
        });


        // 注册需要写在onCreate或Fragment onAttach里，否则会报java.lang.IllegalStateException异常
        launcherResult = createActivityResultLauncher();

        // 请选择您的初始化方式
        initAccessToken();
//        initAccessTokenWithAkSk();
    }

    private int mSize = 0;
    private StringBuilder sb = new StringBuilder();

    private void getImageText(int index) {
        String imagePath = getImagePath(index);
        if (imagePath != null)
            RecognizeService.recAccurateBasic(MainActivity.this, imagePath,
                    new RecognizeService.ServiceListener() {
                        @Override
                        public void onResult(String result) {
                            Gson gson = new Gson();
                            ImgTextEntity imgTextEntity = gson.fromJson(result, ImgTextEntity.class);
                            int words_result_num = imgTextEntity.getWords_result_num();
                            if (words_result_num > 0) {
                                List<ImgTextEntity.WordsResultBean> words_result = imgTextEntity.getWords_result();
                                sb.append("图" + index + " begin  :  ");
                                for (ImgTextEntity.WordsResultBean wordsBean : words_result) {
                                    sb.append(wordsBean.getWords());
                                    sb.append(";");
                                }
                                sb.append("图" + index + "  end  ");
                            } else {
                                sb.append("图" + index + " 无法识别 ");
                            }

                            sb.append("\n");

                            if (index == mSize - 1) {//最后一张
                                imgText.setText(sb.toString());
                                ProgressUtils.dismissProgressDialog(dialogLoad);
                            } else {// 不是最后一张
                                getImageText(index + 1);
                            }


//                            infoPopText(result);
                        }
                    });
    }

    private void infoPopText(final String result) {
        alertText("", result);
    }


    private String getImagePath(int index) {
        String filepath = null;
        LocalMedia localMedia = selectList.get(index);
        if (AppUtil.isAndroidQ()) {
            filepath = localMedia.getAndroidQToPath();
        } else {
            filepath = localMedia.getRealPath();
        }
        return filepath;
    }

    private boolean checkTokenStatus() {
        if (!hasGotToken) {
            Toast.makeText(getApplicationContext(), "token还未成功获取", Toast.LENGTH_LONG).show();
        }
        return hasGotToken;
    }


    /**
     * 以license文件方式初始化
     */
    private void initAccessToken() {
        OCR.getInstance(this).initAccessToken(new OnResultListener<AccessToken>() {
            @Override
            public void onResult(AccessToken accessToken) {
                String token = accessToken.getAccessToken();
                alertText("获取Token", token);
                hasGotToken = true;
            }

            @Override
            public void onError(OCRError error) {
                error.printStackTrace();
                alertText("licence方式获取token失败", error.getMessage());
            }
        }, getApplicationContext());

    }

    /**
     * 用明文ak，sk初始化
     */
    private void initAccessTokenWithAkSk() {
        OCR.getInstance(this).initAccessTokenWithAkSk(new OnResultListener<AccessToken>() {
            @Override
            public void onResult(AccessToken result) {
                String token = result.getAccessToken();
                alertText("获取Token", token);
                hasGotToken = true;
            }

            @Override
            public void onError(OCRError error) {
                error.printStackTrace();
                alertText("AK，SK方式获取token失败", error.getMessage());
            }
        }, getApplicationContext(), "0ocBxiZTrcuphliveG2qICmX", "Dl2WUbwOTfC4SHGUX4NOrlRiNzcdUTiE");
    }


    private void alertText(final String title, final String message) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                alertDialog.setTitle(title)
                        .setMessage(message)
                        .setPositiveButton("确定", null)
                        .show();
            }
        });
    }


    private final GridImageAdapter.onAddPicClickListener onAddPicClickListener = new GridImageAdapter.onAddPicClickListener() {
        @Override
        public void onAddPicClick() {
            boolean mode = true;// 进入相册 或者 拍照

//            PictureSelector.create(SignPicActivity.this)
//                    .openGallery(PictureMimeType.ofAudio())// 全部.PictureMimeType.ofAll()、图片.ofImage()、视频.ofVideo()、音频.ofAudio()
//                    .imageEngine(GlideEngine.createGlideEngine())// 外部传入图片加载引擎，必传项
//                    .forResult(launcherResult);
            if (mode) {
                // 进入相册 以下是例子：不需要的api可以不写
                PictureSelector.create(MainActivity.this)
                        .openGallery(PictureMimeType.ofImage())// 全部.PictureMimeType.ofAll()、图片.ofImage()、视频.ofVideo()、音频.ofAudio()
                        .imageEngine(GlideEngine.createGlideEngine())// 外部传入图片加载引擎，必传项
                        .maxSelectNum(maxSelectNum)// 最大图片选择数量
                        .isEditorImage(false)//是否编辑图片
                        .selectionMode(PictureConfig.MULTIPLE)// 多选 PictureConfig.MULTIPLE or 单选 PictureConfig.SINGLE
                        .isPreviewImage(true)// 是否可预览图片
                        .isCamera(true)// 是否显示拍照按钮
                        .isCompress(false)// 是否压缩
                        .isZoomAnim(true)// 图片列表点击 缩放效果 默认true
                        .setCameraImageFormat(PictureMimeType.JPEG) // 相机图片格式后缀,默认.jpeg
                        .isEnableCrop(false)// 是否裁剪
                        .isWeChatStyle(false)// 是否开启微信图片选择风格
                        .isUseCustomCamera(false)// 是否使用自定义相机
                        .setLanguage(-1)// 设置语言，默认中文
                        .isPageStrategy(false)// 是否开启分页策略 & 每页多少条；默认开启
                        .setRecyclerAnimationMode(AnimationType.DEFAULT_ANIMATION)// 列表动画效果
                        .isWithVideoImage(true)// 图片和视频是否可以同选,只在ofAll模式下有效
                        .isMaxSelectEnabledMask(false)// 选择数到了最大阀值列表是否启用蒙层效果
                        .setCustomCameraFeatures(CustomCameraView.BUTTON_STATE_BOTH)// 设置自定义相机按钮状态
                        .setCaptureLoadingColor(ContextCompat.getColor(MainActivity.this, R.color.app_color_blue))
                        .minSelectNum(1)// 最小选择数量
                        .maxVideoSelectNum(1) // 视频最大选择数量
                        .imageSpanCount(4)// 每行显示个数
                        .isReturnEmpty(false)// 未选择数据时点击按钮是否可以返回
                        .closeAndroidQChangeWH(true)//如果图片有旋转角度则对换宽高,默认为true
                        .closeAndroidQChangeVideoWH(!SdkVersionUtils.checkedAndroid_Q())// 如果视频有旋转角度则对换宽高,默认为false
                        .isAndroidQTransform(true)// 是否需要处理Android Q 拷贝至应用沙盒的操作，只针对compress(false); && .isEnableCrop(false);有效,默认处理
                        .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)// 设置相册Activity方向，不设置默认使用系统
                        .isOriginalImageControl(false)// 是否显示原图控制按钮，如果设置为true则用户可以自由选择是否使用原图，裁剪功能将会失效
                        .isDisplayOriginalSize(true)// 是否显示原文件大小，isOriginalImageControl true有效
                        .isSingleDirectReturn(true)// 单选模式下是否直接返回，PictureConfig.SINGLE模式下有效
                        .isPreviewVideo(true)// 是否可预览视频
                        .isEnablePreviewAudio(true) // 是否可播放音频
                        .setCameraVideoFormat(PictureMimeType.MP4)// 相机视频格式后缀,默认.mp4
                        .setCameraAudioFormat(PictureMimeType.AMR)// 录音音频格式后缀,默认.amr
                        .isCompress(false)// 是否压缩
                        .synOrAsy(false)//同步true或异步false 压缩 默认同步
                        .isCropDragSmoothToCenter(true)// 裁剪框拖动时图片自动跟随居中
                        .circleDimmedLayer(false)// 是否圆形裁剪
                        .showCropFrame(false)// 是否显示裁剪矩形边框 圆形裁剪时建议设为false
                        .showCropGrid(false)// 是否显示裁剪矩形网格 圆形裁剪时建议设为false
                        .isOpenClickSound(false)// 是否开启点击声音
                        .selectionData(mAdapter.getData())// 是否传入已选图片
                        .cutOutQuality(90)// 裁剪输出质量 默认100
                        .minimumCompressSize(100)// 小于多少kb的图片不压缩
                        .forResult(launcherResult);

            } else {
                // 单独拍照
                PictureSelector.create(MainActivity.this)
                        .openCamera(PictureMimeType.ofImage())// 单独拍照，也可录像或也可音频 看你传入的类型是图片or视频 // 全部.PictureMimeType.ofAll()、图片.ofImage()、视频.ofVideo()、音频.ofAudio()
                        .theme(R.style.picture_default_style)// 主题样式设置 具体参考 values/styles
                        .imageEngine(GlideEngine.createGlideEngine())// 外部传入图片加载引擎，必传项
//                        .setPictureWindowAnimationStyle(mWindowAnimationStyle)// 自定义相册启动退出动画
                        .maxSelectNum(maxSelectNum)// 最大图片选择数量
                        .isUseCustomCamera(true)// 是否使用自定义相机
                        //.setOutputCameraPath()// 自定义相机输出目录
                        .minSelectNum(1)// 最小选择数量
                        //.querySpecifiedFormatSuffix(PictureMimeType.ofPNG())// 查询指定后缀格式资源
                        .closeAndroidQChangeWH(true)//如果图片有旋转角度则对换宽高，默认为true
                        .closeAndroidQChangeVideoWH(!SdkVersionUtils.checkedAndroid_Q())// 如果视频有旋转角度则对换宽高，默认false
                        .selectionMode(PictureConfig.MULTIPLE)// 多选 or 单选 PictureConfig.MULTIPLE : PictureConfig.SINGLE
                        //.cameraFileName(System.currentTimeMillis() + ".jpg")// 使用相机时保存至本地的文件名称,注意这个只在拍照时可以使用
                        //.renameCompressFile(System.currentTimeMillis() + ".jpg")// 重命名压缩文件名、 注意这个不要重复，只适用于单张图压缩使用
                        //.renameCropFileName(System.currentTimeMillis() + ".jpg")// 重命名裁剪文件名、 注意这个不要重复，只适用于单张图裁剪使用
                        .loadCacheResourcesCallback(GlideCacheEngine.createCacheEngine())// 获取图片资源缓存，主要是解决华为10部分机型在拷贝文件过多时会出现卡的问题，这里可以判断只在会出现一直转圈问题机型上使用
                        .isPreviewImage(true)// 是否可预览图片
                        .isPreviewVideo(true)// 是否可预览视频
                        .isEnablePreviewAudio(false) // 是否可播放音频
                        .isCamera(false)// 是否显示拍照按钮
                        .isAndroidQTransform(true)// 是否需要处理Android Q 拷贝至应用沙盒的操作，只针对compress(false); && .isEnableCrop(false);有效,默认处理
                        .isEnableCrop(false)// 是否裁剪
                        //.basicUCropConfig()//对外提供所有UCropOptions参数配制，但如果PictureSelector原本支持设置的还是会使用原有的设置
                        .isCompress(false)// 是否压缩
                        .compressQuality(60)// 图片压缩后输出质量
                        .glideOverride(160, 160)// glide 加载宽高，越小图片列表越流畅，但会影响列表图片浏览的清晰度
//                        .withAspectRatio(aspect_ratio_x, aspect_ratio_y)// 裁剪比例 如16:9 3:2 3:4 1:1 可自定义
//                        .hideBottomControls(!cb_hide.isChecked())// 是否显示uCrop工具栏，默认不显示
//                        .isGif(cb_isGif.isChecked())// 是否显示gif图片
//                        .freeStyleCropEnabled(cb_styleCrop.isChecked())// 裁剪框是否可拖拽
                        .freeStyleCropMode(OverlayView.DEFAULT_FREESTYLE_CROP_MODE)// 裁剪框拖动模式
//                        .circleDimmedLayer(cb_crop_circular.isChecked())// 是否圆形裁剪
                        //.setCircleDimmedColor(ContextCompat.getColor(this, R.color.app_color_white))// 设置圆形裁剪背景色值
                        //.setCircleDimmedBorderColor(ContextCompat.getColor(this, R.color.app_color_white))// 设置圆形裁剪边框色值
                        //.setCircleStrokeWidth(3)// 设置圆形裁剪边框粗细
//                        .showCropFrame(cb_showCropFrame.isChecked())// 是否显示裁剪矩形边框 圆形裁剪时建议设为false
//                        .showCropGrid(cb_showCropGrid.isChecked())// 是否显示裁剪矩形网格 圆形裁剪时建议设为false
//                        .isOpenClickSound(cb_voice.isChecked())// 是否开启点击声音
                        .selectionData(mAdapter.getData())// 是否传入已选图片
                        .isAutoScalePreviewImage(true)// 如果图片宽度不能充满屏幕则自动处理成充满模式
                        //.isPreviewEggs(true)// 预览图片时 是否增强左右滑动图片体验(图片滑动一半即可看到上一张是否选中)
                        //.cropCompressQuality(90)// 废弃 改用cutOutQuality()
                        .cutOutQuality(90)// 裁剪输出质量 默认100
                        .minimumCompressSize(100)// 小于100kb的图片不压缩
                        //.cropWH()// 裁剪宽高比，设置如果大于图片本身宽高则无效
                        //.cropImageWideHigh()// 裁剪宽高比，设置如果大于图片本身宽高则无效
                        //.rotateEnabled() // 裁剪是否可旋转图片
                        //.scaleEnabled()// 裁剪是否可放大缩小图片
                        //.videoQuality()// 视频录制质量 0 or 1
                        //.forResult(PictureConfig.CHOOSE_REQUEST);//结果回调onActivityResult code
                        .forResult(new MyResultCallback(mAdapter));
            }
        }
    };


    /**
     * 返回结果回调
     */
    private static class MyResultCallback implements OnResultCallbackListener<LocalMedia> {
        private WeakReference<GridImageAdapter> mAdapterWeakReference;

        public MyResultCallback(GridImageAdapter adapter) {
            super();
            this.mAdapterWeakReference = new WeakReference<>(adapter);
        }

        @Override
        public void onResult(List<LocalMedia> result) {
            for (LocalMedia media : result) {
                if (media.getWidth() == 0 || media.getHeight() == 0) {
                    if (PictureMimeType.isHasImage(media.getMimeType())) {
                        MediaExtraInfo imageExtraInfo = MediaUtils.getImageSize(media.getPath());
                        media.setWidth(imageExtraInfo.getWidth());
                        media.setHeight(imageExtraInfo.getHeight());
                    } else if (PictureMimeType.isHasVideo(media.getMimeType())) {
                        MediaExtraInfo videoExtraInfo = MediaUtils.getVideoSize(PictureAppMaster.getInstance().getAppContext(), media.getPath());
                        media.setWidth(videoExtraInfo.getWidth());
                        media.setHeight(videoExtraInfo.getHeight());
                    }
                }
                Log.i(TAG, "文件名: " + media.getFileName());
                Log.i(TAG, "是否压缩:" + media.isCompressed());
                Log.i(TAG, "压缩:" + media.getCompressPath());
                Log.i(TAG, "原图:" + media.getPath());
                Log.i(TAG, "绝对路径:" + media.getRealPath());
                Log.i(TAG, "是否裁剪:" + media.isCut());
                Log.i(TAG, "裁剪:" + media.getCutPath());
                Log.i(TAG, "是否开启原图:" + media.isOriginal());
                Log.i(TAG, "原图路径:" + media.getOriginalPath());
                Log.i(TAG, "Android Q 特有Path:" + media.getAndroidQToPath());
                Log.i(TAG, "宽高: " + media.getWidth() + "x" + media.getHeight());
                Log.i(TAG, "Size: " + media.getSize());

                Log.i(TAG, "onResult: " + media.toString());

                // TODO 可以通过PictureSelectorExternalUtils.getExifInterface();方法获取一些额外的资源信息，如旋转角度、经纬度等信息
            }
            if (mAdapterWeakReference.get() != null) {
                mAdapterWeakReference.get().setList(result);
                mAdapterWeakReference.get().notifyDataSetChanged();
            }
        }

        @Override
        public void onCancel() {
            Log.i(TAG, "PictureSelector Cancel");
        }
    }

    /**
     * 创建一个ActivityResultLauncher 图片放大后处理结果回调
     *
     * @return
     */


    private ActivityResultLauncher<Intent> createActivityResultLauncher() {
        return registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        int resultCode = result.getResultCode();
                        if (resultCode == RESULT_OK) {
                            selectList = PictureSelector.obtainMultipleResult(result.getData());
                            // 例如 LocalMedia 里面返回五种path
                            // 1.media.getPath(); 原图path
                            // 2.media.getCutPath();裁剪后path，需判断media.isCut();切勿直接使用
                            // 3.media.getCompressPath();压缩后path，需判断media.isCompressed();切勿直接使用
                            // 4.media.getOriginalPath()); media.isOriginal());为true时此字段才有值
                            // 5.media.getAndroidQToPath();Android Q版本特有返回的字段，但如果开启了压缩或裁剪还是取裁剪或压缩路径；注意：.isAndroidQTransform 为false 此字段将返回空
                            // 如果同时开启裁剪和压缩，则取压缩路径为准因为是先裁剪后压缩
                            for (LocalMedia media : selectList) {
                                if (media.getWidth() == 0 || media.getHeight() == 0) {
                                    if (PictureMimeType.isHasImage(media.getMimeType())) {
                                        MediaExtraInfo imageExtraInfo = MediaUtils.getImageSize(media.getPath());
                                        media.setWidth(imageExtraInfo.getWidth());
                                        media.setHeight(imageExtraInfo.getHeight());
                                    } else if (PictureMimeType.isHasVideo(media.getMimeType())) {
                                        MediaExtraInfo videoExtraInfo = MediaUtils.getVideoSize(MainActivity.this, media.getPath());
                                        media.setWidth(videoExtraInfo.getWidth());
                                        media.setHeight(videoExtraInfo.getHeight());
                                    }
                                }
                                Log.i(TAG, "是否压缩:" + media.isCompressed());
                                Log.i(TAG, "压缩:" + media.getCompressPath());
                                Log.i(TAG, "原图:" + media.getPath());
                                Log.i(TAG, "绝对路径:" + media.getRealPath());
                                Log.i(TAG, "是否裁剪:" + media.isCut());
                                Log.i(TAG, "裁剪:" + media.getCutPath());
                                Log.i(TAG, "是否开启原图:" + media.isOriginal());
                                Log.i(TAG, "原图路径:" + media.getOriginalPath());
                                Log.i(TAG, "Android Q 特有Path:" + media.getAndroidQToPath());
                                Log.i(TAG, "宽高: " + media.getWidth() + "x" + media.getHeight());
                                Log.i(TAG, "Size: " + media.getSize());

                                // TODO 可以通过PictureSelectorExternalUtils.getExifInterface();方法获取一些额外的资源信息，如旋转角度、经纬度等信息
                            }

                            //刷新列表
                            mAdapter.setList(selectList);
                            mAdapter.notifyDataSetChanged();
                        }
                    }
                });
    }


}