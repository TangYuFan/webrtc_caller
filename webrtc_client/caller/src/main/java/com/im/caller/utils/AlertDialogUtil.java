package com.im.caller.utils;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.im.caller.R;

import java.util.function.Consumer;


/**
 * @desc : 弹出文本输入框，获取文本输入
 * @auth : tyf
 * @date : 2024-10-12 17:31:04
 */
public class AlertDialogUtil {

    private static String TAG = AlertDialogUtil.class.getName();

    // 弹出窗输入文本
    public static void alertInputDialog(Activity activity, String title, String hint, Consumer<String> confirm, Consumer<String> cancel) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        // 加载自定义标题布局
        LayoutInflater inflater = activity.getLayoutInflater();
        View titleView = inflater.inflate(com.im.caller.R.layout.dialog_title, null);
        TextView titleTextView = titleView.findViewById(com.im.caller.R.id.dialogTitle);
        titleTextView.setText(title);  // 设置标题文字
        // 设置自定义标题
        builder.setCustomTitle(titleView);
        // 设置输入框
        final EditText input = new EditText(activity);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint(hint);  // 设置提示文字
        input.setTextSize(15);
        // 设置输入框的边距
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(50, 0, 50, 0);  // 左、上、右、下的边距
        input.setLayoutParams(params);
        // 设置输入框在对话框中的位置
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(input);
        layout.setGravity(Gravity.CENTER);
        builder.setView(layout);
        // 确认
        builder.setPositiveButton("确定", (dialog, which) -> {
            String text = input.getText().toString();
            Log.d(TAG, "text：" + text);
            if (confirm != null && (text!=null&&!"".equals(text.trim()))) {
                confirm.accept(text);
            }
        });
        // 取消
        builder.setNegativeButton("取消", (dialog, which) -> {
            String text = input.getText().toString();
            Log.d(TAG, "text：" + text);
            dialog.cancel();
            if (cancel != null) {
                cancel.accept(text);
            }
        });
        // 禁用点击外部关闭弹窗
        builder.setCancelable(false);
        // 显示弹出窗
        AlertDialog dialog = builder.show();
        // 修改按钮的文字大小
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextSize(15);  // 设置“确定”按钮的文字大小
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextSize(15);  // 设置“取消”按钮的文字大小
        // 获取按钮的文字颜色并应用到标题
        int buttonTextColor = dialog.getButton(DialogInterface.BUTTON_POSITIVE).getCurrentTextColor();
        titleTextView.setTextColor(buttonTextColor);  // 将标题文字颜色设置为与按钮相同
        titleTextView.setTextSize(16);
    }

    // 弹出呼叫窗
    public static void showCallerDialog(Activity activity, String userId,Consumer<Boolean> accept) {

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        LinearLayout dialogLayout = new LinearLayout(activity);
        dialogLayout.setOrientation(LinearLayout.HORIZONTAL);
        dialogLayout.setGravity(Gravity.CENTER_VERTICAL);
        dialogLayout.setPadding(20, 10, 20, 10); // 减少上下的padding

        // 禁用点击外部关闭弹窗
        builder.setCancelable(false);

        // 左边布局
        LinearLayout leftLayout = new LinearLayout(activity);
        leftLayout.setOrientation(LinearLayout.HORIZONTAL);
        leftLayout.setGravity(Gravity.CENTER_VERTICAL);

        // 头像图片
        ImageView avatarImage = new ImageView(activity);
        avatarImage.setImageDrawable(activity.getResources().getDrawable(R.drawable.webrtc_avatar_default)); // 替换为你的头像资源
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(
                (int) (70 * activity.getResources().getDisplayMetrics().density), // 调整头像宽高
                (int) (70 * activity.getResources().getDisplayMetrics().density));
        avatarImage.setLayoutParams(avatarParams);
        avatarImage.setPadding(30, 30, 10, 30); // 头像和文本之间的间距

        // 双行文本
        LinearLayout textLayout = new LinearLayout(activity);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        TextView userIdText = new TextView(activity);
        userIdText.setText(userId);
        TextView invitationText = new TextView(activity);
        invitationText.setText("邀请你通话");

        // 为双行文本设置合理的padding
        userIdText.setPadding(0, 0, 0, 0);
        invitationText.setPadding(0, 0, 0, 0);

        // 设置文本的最大行数，防止内容过多时增加高度
        userIdText.setMaxLines(1);
        invitationText.setMaxLines(1);

        textLayout.addView(userIdText);
        textLayout.addView(invitationText);

        leftLayout.addView(avatarImage);
        leftLayout.addView(textLayout);

        LinearLayout.LayoutParams leftLayoutParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.7f); // 确保左侧使用wrap_content
        leftLayout.setLayoutParams(leftLayoutParams);

        // 右边布局
        LinearLayout rightLayout = new LinearLayout(activity);
        rightLayout.setOrientation(LinearLayout.HORIZONTAL);
        rightLayout.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        rightLayout.setPadding(20, 30, 0, 30); // 增加左边内边距

        // 拒绝按钮
        ImageView rejectButton = new ImageView(activity);
        rejectButton.setImageDrawable(activity.getResources().getDrawable(R.drawable.webrtc_cancel));
        rejectButton.setContentDescription("拒绝");
        LinearLayout.LayoutParams rejectParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                (int) (40 * activity.getResources().getDisplayMetrics().density)); // 调整高度为30dp
        rejectButton.setLayoutParams(rejectParams);
        rejectButton.setPadding(5, 0, 5, 0);

        // 接听按钮
        ImageView acceptButton = new ImageView(activity);
        acceptButton.setImageDrawable(activity.getResources().getDrawable(R.drawable.webrtc_answer));
        acceptButton.setContentDescription("接听");
        LinearLayout.LayoutParams acceptParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                (int) (40 * activity.getResources().getDisplayMetrics().density)); // 调整高度为30dp
        acceptButton.setLayoutParams(acceptParams);
        acceptButton.setPadding(5, 0, 5, 0);

        rightLayout.addView(rejectButton);
        rightLayout.addView(acceptButton);

        dialogLayout.addView(leftLayout);
        dialogLayout.addView(rightLayout);

        builder.setView(dialogLayout);
        AlertDialog dialog = builder.create();
        dialog.show();

        // 点击事件
        View.OnClickListener buttonClickListener = v -> {
            dialog.dismiss();
            if(accept==null){
                return;
            }
            if (v == rejectButton) {
                // 拒绝通话逻辑
                accept.accept(false);
            } else if (v == acceptButton) {
                // 接听通话逻辑
                accept.accept(true);
            }
        };

        rejectButton.setOnClickListener(buttonClickListener);
        acceptButton.setOnClickListener(buttonClickListener);
    }






}
