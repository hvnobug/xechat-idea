package cn.xeblog.plugin.action.handler.message;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.thread.GlobalThreadPool;
import cn.xeblog.commons.constants.IpConstants;
import cn.xeblog.commons.entity.IpRegion;
import cn.xeblog.commons.entity.Response;
import cn.xeblog.commons.entity.User;
import cn.xeblog.commons.entity.UserMsgDTO;
import cn.xeblog.commons.entity.react.React;
import cn.xeblog.commons.entity.react.request.DownloadReact;
import cn.xeblog.commons.entity.react.result.DownloadReactResult;
import cn.xeblog.commons.enums.MessageType;
import cn.xeblog.plugin.action.ConsoleAction;
import cn.xeblog.plugin.action.ReactAction;
import cn.xeblog.plugin.action.handler.ReactResultConsumer;
import cn.xeblog.plugin.annotation.DoMessage;
import cn.xeblog.plugin.cache.DataCache;
import cn.xeblog.plugin.enums.Style;
import cn.xeblog.plugin.util.NotifyUtils;
import com.intellij.ide.actions.OpenFileAction;
import com.intellij.openapi.application.ApplicationManager;

import javax.swing.*;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileOutputStream;

/**
 * @author anlingyi
 * @date 2020/8/19
 */
@DoMessage(MessageType.USER)
public class UserMessageHandler extends AbstractMessageHandler<UserMsgDTO> {

    private static final String IMAGES_DIR = System.getProperty("user.home") + "/xechat/images";

    @Override
    protected void process(Response<UserMsgDTO> response) {
        User user = response.getUser();
        UserMsgDTO body = response.getBody();
        boolean isImage = body.getMsgType() == UserMsgDTO.MsgType.IMAGE;
        if (isImage) {
            String fileName = (String) body.getContent();
            JLabel imgLabel = new JLabel("下载图片");
            imgLabel.setToolTipText("点击下载图片");
            imgLabel.setAlignmentY(0.85f);
            imgLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            imgLabel.setForeground(StyleConstants.getForeground(Style.DEFAULT.get()));
            imgLabel.addMouseListener(new MouseAdapter() {
                MouseListener mouseListener = this;
                @Override
                public void mouseClicked(MouseEvent e) {
                    imgLabel.setEnabled(false);
                    imgLabel.setText("图片下载中...");
                    imgLabel.setToolTipText("图片下载中...");

                    GlobalThreadPool.execute(() -> {
                        ReactAction.request(new DownloadReact(fileName), React.DOWNLOAD, new ReactResultConsumer<DownloadReactResult>() {
                            @Override
                            public void succeed(DownloadReactResult body) {
                                String filePath = IMAGES_DIR + "/" + fileName;
                                File imageFile = new File(filePath);
                                if (!imageFile.exists()) {
                                    FileUtil.mkdir(IMAGES_DIR);
                                    try (FileOutputStream out = new FileOutputStream(imageFile)) {
                                        out.write(body.getBytes());
                                    } catch (Exception exception) {
                                        exception.printStackTrace();
                                    }
                                }

                                imgLabel.removeMouseListener(mouseListener);
                                imgLabel.addMouseListener(new MouseAdapter() {
                                    @Override
                                    public void mouseClicked(MouseEvent e) {
                                        ApplicationManager.getApplication().invokeLater(() -> {
                                            OpenFileAction.openFile(filePath, DataCache.project);
                                        });
                                    }
                                });

                                imgLabel.setEnabled(true);
                                imgLabel.setText("查看图片");
                                imgLabel.setToolTipText("点击查看图片");

                                ConsoleAction.updateUI();
                            }

                            @Override
                            public void failed(String msg) {
                                imgLabel.setEnabled(true);
                                imgLabel.setText("重新下载");
                                imgLabel.setToolTipText("点击重新下载");
                            }
                        });
                    });
                }
            });

            ConsoleAction.atomicExec(() -> {
                renderName(response);
                ConsoleAction.renderImageLabel(imgLabel);
            });
        } else {
            ConsoleAction.atomicExec(() -> {
                renderName(response);
                boolean notified = body.hasUser(DataCache.username);
                Style style = Style.DEFAULT;
                String msg = (String) body.getContent();
                if (notified) {
                    style = Style.LIGHT;
                    if (!user.getUsername().equals(DataCache.username)) {
                        NotifyUtils.info(user.getUsername(), msg);
                    }
                }
                ConsoleAction.renderText(msg + "\n", style);
            });
        }
    }

    private void renderName(Response<UserMsgDTO> response) {
        User user = response.getUser();
        IpRegion region = user.getRegion();
        final String shortProvince = MapUtil.getStr(IpConstants.SHORT_PROVINCE, region.getProvince(), region.getCountry());
        String roleDisplay = "";
        if (user.getRole() == User.Role.ADMIN) {
            roleDisplay = " ☆";
        }

        ConsoleAction.renderText(
                String.format("[%s][%s] %s (%s)%s：", response.getTime(), shortProvince, user.getUsername(),
                        user.getStatus().getName(), roleDisplay), Style.USER_NAME);
    }

}
