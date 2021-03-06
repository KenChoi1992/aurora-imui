## ChatInput
这是一个聊天界面输入框组件，可以方便地结合 imui 使用，包含录音，选择图片，拍照等功能，提供了一些丰富的接口和回调供用户使用，
还可以选择自定义样式。

## 用法
使用 ChatInput 只需要两个步骤。

#### 在 xml 布局文件中引用 ChatInputView：

```
    <cn.jiguang.imui.chatinput.ChatInputView
        android:id="@+id/chat_input"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_alignParentBottom="true"
        app:cameraBtnIcon="@drawable/camera"
        app:inputCursorDrawable="@drawable/jmui_text_cursor_bg"
        app:inputEditTextBg="@drawable/jmui_chat_input_bg"
        app:inputHint="@string/input_hint"
        app:photoBtnIcon="@drawable/photo"
        app:sendBtnIcon="@drawable/send"
        app:voiceBtnIcon="@drawable/mic" />

```

#### 初始化 ChatInputView

```
ChatInputView chatInputView = (ChatInputView) findViewById(R.id.chat_input);
chatInputView.setMenuContainerHeight(softInputHeight);
```
初始化后一定要设置一下 MenuContainer 的高度，最好设置为软键盘的高度，否则会导致第一次打开菜单时高度不正常（此时
打开软键盘会导致界面伸缩）。建议在跳转到聊天界面之前使用 onSizeChanged 方法监听软键盘的高度，然后在初始化的时候设置即可，
关于监听软键盘高度的方法可以参考 sample 下的 MessageListActivity 及 ChatView 中的 onSizeChanged 相关方法。

## 重要接口及事件
ChatInputView 提供了各种按钮及事件的监听回调，所以用户可以灵活地运用监听事件做一些操作，如发送消息等事件。

#### OnMenuClickListener
首先是输入框下面的菜单栏事件的监听，调用 chatInputView.setMenuClickListener 即可设置监听：
```
chatInput.setMenuClickListener(new ChatInputView.OnMenuClickListener() {
    @Override
    public boolean onSubmit(CharSequence input) {
         // 点击发送按钮事件
    }

    @Override
    public void onSendFiles(List<String> list) {
        // 选中文件后，点击发送按钮触发此事件
    }

    @Override
    public void onVoiceClick() {
        // 点击语音按钮触发事件
    }

    @Override
    public void onPhotoClick() {
        // 点击图片按钮触发事件
    }

    @Override
    public void onCameraClick() {
        // 点击拍照按钮触发事件
    }
    
     @Override
     public void onVideoRecordFinished(String filePath) {
         // 录完视频后，点击发送触发事件
     }
});
```
关于上述事件的处理，可以参考 sample 中的 MessageListActivity 对于事件的处理。

#### RecordVoiceButton.RecordVoiceListener
这是录音的接口，使用方式：

```
mRecordVoiceBtn = mChatInput.getRecordVoiceButton();
mRecordVoiceBtn.setRecordVoiceListener(new RecordVoiceButton.RecordVoiceListener() {
    @Override
    public void onStartRecord() {
        // Show record voice interface
        // 设置存放录音文件目录
        File rootDir = mContext.getFilesDir();
        String fileDir = rootDir.getAbsolutePath() + "/voice";
        mRecordVoiceBtn.setRecordVoiceFile(fileDir, new DateFormat().format("yyyy_MMdd_hhmmss",
                Calendar.getInstance(Locale.CHINA)) + "");
    }

    @Override
    public void onFinishRecord(File voiceFile, int duration) {
        MyMessage message = new MyMessage(null, IMessage.MessageType.SEND_VOICE);
        message.setContentFile(voiceFile.getPath());
        message.setDuration(duration);
        mAdapter.addToStart(message, true);
    }

    @Override
    public void onCancelRecord() {

    }
});
```

#### 设置拍照后保存的文件
setCameraCaptureFile(String path, String fileName)

```
// 参数分别是路径及文件名，建议在上面的 onCameraClick 触发时调用
mChatInput.setCameraCaptureFile(path, fileName);

```
