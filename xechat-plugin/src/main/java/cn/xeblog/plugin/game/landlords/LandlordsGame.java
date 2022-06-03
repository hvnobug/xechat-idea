package cn.xeblog.plugin.game.landlords;

import cn.hutool.core.bean.BeanUtil;
import cn.xeblog.commons.entity.game.landlords.LandlordsGameDTO;
import cn.xeblog.commons.entity.game.landlords.Poker;
import cn.xeblog.commons.entity.game.landlords.PokerInfo;
import cn.xeblog.plugin.cache.DataCache;
import cn.xeblog.plugin.game.AbstractGame;
import com.intellij.ui.components.JBScrollPane;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author anlingyi
 * @date 2022/6/2 1:13 下午
 */
public class LandlordsGame extends AbstractGame<LandlordsGameDTO> {

    private JPanel startPanel;

    private JButton outPokerButton;
    private JButton notOutPokerButton;
    private JLabel tipsLabel;
    private JPanel outPokerPanel;

    private Map<String, Player> playerMap;

    /**
     * 状态 0.初始化 1.摸牌 2.叫分 3.出牌 4.失败 5.胜利
     */
    private int state;

    /**
     * 当前玩家
     */
    private PlayerNode currentPlayer;

    /**
     * 玩家手牌
     */
    private List<Poker> pokers;

    /**
     * 已选中的牌
     */
    private List<Poker> selectedPokers = new ArrayList<>();

    /**
     * 当前出牌信息
     */
    private PokerInfo pokerInfo;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private class Player {
        private PlayerNode playerNode;
        private JPanel panel;
    }

    @Override
    protected void init() {
        state = 0;
        playerMap = null;
        currentPlayer = null;

        mainPanel.removeAll();
        mainPanel.setLayout(null);
        mainPanel.setEnabled(true);
        mainPanel.setVisible(true);
        mainPanel.setMinimumSize(new Dimension(150, 200));
        startPanel = new JPanel();
        startPanel.setBounds(10, 10, 120, 200);
        mainPanel.add(startPanel);

        JLabel title = new JLabel("斗地主！");
        title.setFont(new Font("", 1, 14));
        startPanel.add(title);

        Box vBox = Box.createVerticalBox();
        startPanel.add(vBox);

        vBox.add(Box.createVerticalStrut(20));
        vBox.add(getStartGameButton());
        if (DataCache.isOnline) {
            List<Integer> numsList = new ArrayList();
            numsList.add(2);
            numsList.add(3);
            vBox.add(getCreateRoomButton(numsList));
        }
        vBox.add(getExitButton());

        mainPanel.updateUI();
    }

    @Override
    protected void start() {
        playerMap = new HashMap<>();

        PlayerNode playerNode1 = new PlayerNode("玩家1");
        playerNode1.setRole(1);
        playerNode1.setPokerTotal(17);
        PlayerNode playerNode2 = new PlayerNode("玩家2");
        playerNode2.setRole(1);
        playerNode2.setPokerTotal(17);
        PlayerNode playerNode3 = new PlayerNode("玩家3");
        playerNode3.setRole(2);
        playerNode3.setPokerTotal(20);

        playerNode1.setPrevPlayer(playerNode3);
        playerNode1.setNextPlayer(playerNode2);

        playerNode2.setPrevPlayer(playerNode1);
        playerNode2.setNextPlayer(playerNode3);

        playerNode3.setPrevPlayer(playerNode2);
        playerNode3.setNextPlayer(playerNode1);

        currentPlayer = playerNode1;

        playerMap.put(playerNode1.getPlayer(), new Player(playerNode1, null));
        playerMap.put(playerNode2.getPlayer(), new Player(playerNode2, null));
        playerMap.put(playerNode3.getPlayer(), new Player(playerNode3, null));

        pokers = PokerUtil.allocPokers().get(0);

        showGamePanel();
    }

    @Override
    public void handle(LandlordsGameDTO body) {

    }

    private void showGamePanel() {
        mainPanel.removeAll();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setMinimumSize(new Dimension(490, 350));

        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(480, 300));
        panel.setLayout(new BorderLayout(10, 10));

        JPanel leftPanel = new JPanel();
        JPanel rightPanel = new JPanel();
        JPanel bottomPanel = new JPanel();
        JPanel centerPanel = new JPanel();

        JPanel textPanel = new JPanel();
        textPanel.setPreferredSize(new Dimension(300, 30));
        tipsLabel = new JLabel();
        textPanel.add(tipsLabel);
        centerPanel.add(textPanel);

        JPanel showPokerPanel = new JPanel(new BorderLayout());
        showPokerPanel.setPreferredSize(new Dimension(200, 70));
        outPokerPanel = new JPanel();
        JBScrollPane pokersScroll = new JBScrollPane(outPokerPanel);
        pokersScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        pokersScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        showPokerPanel.add(pokersScroll);
        centerPanel.add(showPokerPanel);

        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(rightPanel, BorderLayout.EAST);
        panel.add(bottomPanel, BorderLayout.SOUTH);
        panel.add(centerPanel, BorderLayout.CENTER);

        playerMap.get(currentPlayer.getPrevPlayer().getPlayer()).setPanel(leftPanel);
        playerMap.get(currentPlayer.getPlayer()).setPanel(bottomPanel);
        playerMap.get(currentPlayer.getNextPlayer().getPlayer()).setPanel(rightPanel);
        playerMap.forEach((k, v) -> flushPlayerPanel(v));

        JPanel mainTopPanel = new JPanel();
        JLabel title = new JLabel("斗地主！");
        title.setFont(new Font("", 1, 14));
        mainTopPanel.add(title);

        JPanel mainBottomPanel = new JPanel();
        mainBottomPanel.add(getBackButton());

        mainPanel.add(mainTopPanel, BorderLayout.NORTH);
        mainPanel.add(panel, BorderLayout.CENTER);
        mainPanel.add(mainBottomPanel, BorderLayout.SOUTH);
        JPanel placeholderPanel = new JPanel();
        placeholderPanel.setMinimumSize(new Dimension(5, 0));
        mainPanel.add(placeholderPanel, BorderLayout.WEST);
        mainPanel.add(placeholderPanel, BorderLayout.EAST);
        mainPanel.updateUI();
    }

    private void flushPlayerPanel(Player player) {
        PlayerNode playerNode = player.getPlayerNode();
        JPanel panel = player.getPanel();
        panel.removeAll();

        String role = playerNode.getRole() == 1 ? "农民" : "地主";
        JLabel nicknameLabel = new JLabel("<html>" + playerNode.getPlayer() + "</html>");
        JLabel roleLabel = new JLabel(role + "：" + playerNode.getPokerTotal());
        JPanel mainPanel = new JPanel();

        if (playerNode == currentPlayer) {
            mainPanel.setPreferredSize(new Dimension(460, 150));

            JPanel topPanel = new JPanel();
            topPanel.setPreferredSize(new Dimension(460, 40));
            panel.add(topPanel);

            outPokerButton = new JButton("出牌");
            outPokerButton.setEnabled(false);
            outPokerButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (outPokerButton.isEnabled() && pokerInfo != null) {
                        PokerInfo copy = BeanUtil.copyProperties(pokerInfo, PokerInfo.class);
                        System.out.println(copy);
                        outPokerButton.setEnabled(false);
                        pokers.removeAll(selectedPokers);
                        currentPlayer.minusPoker(selectedPokers.size());
                        selectedPokers.clear();
                        flushPlayerPanel(playerMap.get(currentPlayer.getPlayer()));
                        showPokerInfo(copy);
                        pokerInfo = null;
                    }
                }
            });

            notOutPokerButton = new JButton("过！");
            notOutPokerButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {

                }
            });

            topPanel.add(notOutPokerButton);
            topPanel.add(outPokerButton);

            JPanel pokerMainPanel = new JPanel(new BorderLayout());
            pokerMainPanel.setPreferredSize(new Dimension(460, 70));
            JPanel pokerListPanel = new JPanel();
            pokers.forEach(poker -> {
                JPanel pokerPanel = getPokerPanel(poker);
                pokerListPanel.add(pokerPanel);

                pokerPanel.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        boolean selected = selectedPokers.remove(poker);
                        if (selected) {
                            pokerPanel.setBorder(BorderFactory.createLineBorder(new Color(236, 233, 233)));
                        } else {
                            selectedPokers.add(poker);
                            pokerPanel.setBorder(BorderFactory.createLineBorder(new Color(255, 104, 104)));
                        }
                        pokerInfo = PokerUtil.getPokerInfo(selectedPokers);
                        outPokerButton.setEnabled(pokerInfo != null);
                    }
                });
            });
            JBScrollPane pokersScroll = new JBScrollPane(pokerListPanel);
            pokersScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            pokersScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            pokerMainPanel.add(pokersScroll);
            mainPanel.add(pokerMainPanel);
            nicknameLabel.setPreferredSize(new Dimension(300, 30));
            mainPanel.add(nicknameLabel);
            mainPanel.add(roleLabel);
        } else {
            mainPanel.setLayout(new BorderLayout());
            mainPanel.setPreferredSize(new Dimension(120, 100));
            mainPanel.setBorder(BorderFactory.createEtchedBorder());
//            mainPanel.setBorder(BorderFactory.createLineBorder(new Color(168, 0, 0)));
            nicknameLabel.setHorizontalAlignment(SwingConstants.CENTER);
            nicknameLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
            mainPanel.add(nicknameLabel, BorderLayout.NORTH);
            int alignment;
            Border border;
            if (currentPlayer.getNextPlayer() == playerNode) {
                alignment = SwingConstants.LEFT;
                border = BorderFactory.createEmptyBorder(0, 5, 5, 0);
            } else {
                alignment = SwingConstants.RIGHT;
                border = BorderFactory.createEmptyBorder(0, 0, 5, 5);
            }
            roleLabel.setBorder(border);
            roleLabel.setHorizontalAlignment(alignment);
            mainPanel.add(roleLabel, BorderLayout.SOUTH);
        }

        panel.add(mainPanel);
        panel.updateUI();
    }

    private JPanel getPokerPanel(Poker poker) {
        JPanel pokerPanel = new JPanel();
        pokerPanel.setBorder(BorderFactory.createLineBorder(new Color(236, 233, 233)));
        pokerPanel.setPreferredSize(new Dimension(30, 55));
        pokerPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JLabel pokerValue = new JLabel(" " + getPokerDisplayValue(poker.getValue()));
        pokerValue.setPreferredSize(new Dimension(30, 13));
        JLabel pokerSuits = new JLabel(" " + getPokerDisplaySuits(poker.getSuits()));
        pokerSuits.setPreferredSize(new Dimension(30, 13));
        pokerPanel.add(pokerValue);
        pokerPanel.add(pokerSuits);
        return pokerPanel;
    }

    private void showTips(String tips) {
        tipsLabel.setText(tips);
        tipsLabel.updateUI();
    }

    private void showPokerInfo(PokerInfo pokerInfo) {
        showTips("玩家1打出");
        outPokerPanel.removeAll();
        pokerInfo.getPokers().forEach(poker -> outPokerPanel.add(getPokerPanel(poker)));
        outPokerPanel.updateUI();
    }

    private String getPokerDisplayValue(int value) {
        switch (value) {
            case 11:
                return "J";
            case 12:
                return "Q";
            case 13:
                return "K";
            case 14:
                return "A";
            case 15:
                return "2";
            case 16:
                return "王";
            case 17:
                return "皇";
            default:
                return value + "";
        }
    }

    private String getPokerDisplaySuits(Poker.Suits suits) {
        if (suits == null) {
            return "";
        }

        switch (suits) {
            case SPADE:
                return "♠";
            case HEART:
                return "♥";
            case DIAMOND:
                return "♦";
            case CLUB:
                return "♣";
            default:
                return "";
        }
    }

    private JButton getStartGameButton() {
        JButton button = new JButton("开始游戏");
        button.addActionListener(e -> start());
        return button;
    }

    private JButton getBackButton() {
        JButton button = new JButton("返回游戏");
        button.addActionListener(e -> init());
        return button;
    }

}