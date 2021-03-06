package cn.jiguang.imui.messages;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;

import cn.jiguang.imui.BuildConfig;
import cn.jiguang.imui.R;
import cn.jiguang.imui.commons.models.IMessage;
import cn.jiguang.imui.utils.CircleImageView;
import cn.jiguang.imui.utils.DateFormatter;

public class VoiceViewHolder<MESSAGE extends IMessage> extends BaseMessageViewHolder<MESSAGE>
        implements MsgListAdapter.DefaultMessageViewHolder {

    private boolean mIsSender;
    protected TextView mMsgTv;
    protected TextView mDateTv;
    protected CircleImageView mAvatarIv;
    protected ImageView mVoiceIv;
    protected TextView mLengthTv;
    protected ImageView mUnreadStatusIv;
    private boolean mSetData = false;
    private int mPlayPosition = -1;
    private final MediaPlayer mMediaPlayer = new MediaPlayer();
    private AnimationDrawable mVoiceAnimation;
    private FileInputStream mFIS;
    private FileDescriptor mFD;
    private boolean mIsEarPhoneOn;
    private int mSendDrawable;
    private int mReceiveDrawable;
    private int mPlaySendAnim;
    private int mPlayReceiveAnim;

    public VoiceViewHolder(View itemView, boolean isSender) {
        super(itemView);
        this.mIsSender = isSender;
        mMsgTv = (TextView) itemView.findViewById(R.id.message_tv);
        mDateTv = (TextView) itemView.findViewById(R.id.date_tv);
        mAvatarIv = (CircleImageView) itemView.findViewById(R.id.avatar_iv);
        mVoiceIv = (ImageView) itemView.findViewById(R.id.voice_iv);

        if (isSender) {
            mLengthTv = (TextView) itemView.findViewById(R.id.voice_length_tv);
        } else {
            mUnreadStatusIv = (ImageView) itemView.findViewById(R.id.read_status_iv);
        }

        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_RING);
        mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {

            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                return false;
            }
        });
    }

    @Override
    public void onBind(final MESSAGE message) {
        mDateTv.setText(DateFormatter.format(message.getCreatedAt(), DateFormatter.Template.TIME));
        boolean isAvatarExists = message.getUserInfo().getAvatar() != null
                && !message.getUserInfo().getAvatar().isEmpty();
        if (isAvatarExists && mImageLoader != null) {
            mImageLoader.loadImage(mAvatarIv, message.getUserInfo().getAvatar());
        }
        int duration = message.getDuration();
        String lengthStr = duration + mContext.getString(R.string.symbol_second);
        int width = (int) (-0.04 * duration * duration + 4.526 * duration + 75.214);
        mMsgTv.setWidth((int) (width * mDensity));
        mLengthTv.setText(lengthStr);

        mMsgTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mMsgClickListener != null) {
                    mMsgClickListener.onMessageClick(message);
                }
                // 播放中点击了正在播放的Item 则暂停播放
                if (mIsSender) {
                    mVoiceIv.setImageResource(mPlaySendAnim);
                } else {
                    mVoiceIv.setImageResource(mPlayReceiveAnim);
                }

                mVoiceAnimation = (AnimationDrawable) mVoiceIv.getDrawable();
                if (mMediaPlayer.isPlaying() && mPosition == getAdapterPosition()) {
                    pauseVoice();
                    mVoiceAnimation.stop();
                    // 开始播放录音
                } else {
                    // 继续播放之前暂停的录音
                    if (mSetData && mPosition == mPlayPosition) {
                        mVoiceAnimation.start();
                        mMediaPlayer.start();
                        // 否则重新播放该录音或者其他录音
                    } else {
                        playVoice(mPosition, message, true);
                    }
                }
            }
        });

        mMsgTv.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (mMsgLongClickListener != null) {
                    mMsgLongClickListener.onMessageLongClick(message);
                } else {
                    if (BuildConfig.DEBUG) {
                        Log.w("MsgListAdapter", "Didn't set long click listener! Drop event.");
                    }
                }
                return true;
            }
        });

        mAvatarIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mAvatarClickListener != null) {
                    mAvatarClickListener.onAvatarClick(message);
                }
            }
        });
    }

    private void playVoice(int position, MESSAGE message, final boolean isSender) {
        mPosition = position;
        try {
            mMediaPlayer.reset();
            mFIS = new FileInputStream(message.getContentFile());
            mFD = mFIS.getFD();
            mMediaPlayer.setDataSource(mFD);
            if (mIsEarPhoneOn) {
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
            } else {
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            }
            mMediaPlayer.prepare();
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mVoiceAnimation.start();
                    mp.start();
                }
            });
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mVoiceAnimation.stop();
                    mp.reset();
                    mSetData = false;
                    if (isSender) {
                        mVoiceIv.setImageResource(mSendDrawable);
                    } else {
                        mVoiceIv.setImageResource(mReceiveDrawable);
                    }
                }
            });
        } catch (Exception e) {
            Toast.makeText(mContext, mContext.getString(R.string.file_not_found_toast),
                    Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } finally {
            try {
                if (mFIS != null) {
                    mFIS.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void pauseVoice() {
        mMediaPlayer.pause();
        mSetData = true;
    }

    public void setAudioPlayByEarPhone(int state) {
        AudioManager audioManager = (AudioManager) mContext
                .getSystemService(Context.AUDIO_SERVICE);
        int currVolume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
        audioManager.setMode(AudioManager.MODE_IN_CALL);
        if (state == 0) {
            mIsEarPhoneOn = false;
            audioManager.setSpeakerphoneOn(true);
            audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL,
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL),
                    AudioManager.STREAM_VOICE_CALL);
        } else {
            mIsEarPhoneOn = true;
            audioManager.setSpeakerphoneOn(false);
            audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, currVolume,
                    AudioManager.STREAM_VOICE_CALL);
        }
    }

    @Override
    public void applyStyle(MessageListStyle style) {
        mDateTv.setTextSize(style.getDateTextSize());
        mDateTv.setTextColor(style.getDateTextColor());
        mSendDrawable = style.getSendVoiceDrawable();
        mReceiveDrawable = style.getReceiveVoiceDrawable();
        mPlaySendAnim = style.getPlaySendVoiceAnim();
        mPlayReceiveAnim = style.getPlayReceiveVoiceAnim();
        if (mIsSender) {
            mVoiceIv.setImageResource(mSendDrawable);
            mMsgTv.setBackground(style.getSendBubbleDrawable());
        } else {
            mVoiceIv.setImageResource(mReceiveDrawable);
            mMsgTv.setBackground(style.getReceiveBubbleDrawable());
        }

        android.view.ViewGroup.LayoutParams layoutParams = mAvatarIv.getLayoutParams();
        layoutParams.width = style.getAvatarWidth();
        layoutParams.height = style.getAvatarHeight();
        mAvatarIv.setLayoutParams(layoutParams);
    }
}