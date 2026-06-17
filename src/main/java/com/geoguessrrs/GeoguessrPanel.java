package com.geoguessrrs;

import com.geoguessrrs.round.Round;
import com.geoguessrrs.round.RoundResult;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

public class GeoguessrPanel extends PluginPanel
{
	private static final int CLUE_IMAGE_SIZE = 200;
	private static final int MAX_HISTORY = 10;
	private static final Color HINT_BUTTON_COLOR  = new Color(0x4A90D9);
	private static final Color TOGGLE_ACTIVE_BG   = new Color(0x2A5F8F);
	private static final Color TOGGLE_INACTIVE_BG = new Color(0x3B3B3B);

	// Mode / difficulty selectors
	private GameMode selectedMode = GameMode.HUNT;
	private Difficulty selectedDifficulty = Difficulty.RANDOM;
	private final JButton[] modeButtons       = new JButton[GameMode.values().length];
	private final JButton[] difficultyButtons = new JButton[Difficulty.values().length];

	// Top controls
	private final JButton startButton = new JButton("Start Round");
	private final JButton guessButton = new JButton("Guess Here!");

	// Clue image
	private final JLabel clueImageLabel = new JLabel();

	// Hint section
	private final JButton[] hintButtons = new JButton[3];
	private final JTextArea hintText = new JTextArea();

	// Score / history
	private final JLabel scoreLabel = new JLabel("Score: 0");
	private final JLabel totalScoreLabel = new JLabel("Total: 0");
	private final JPanel historyPanel = new JPanel();

	private int totalScore = 0;
	private final Deque<String> history = new LinkedList<>();

	// Callbacks set by GeoguessrPlugin
	private Runnable onStartRound;
	private Runnable onGuess;
	private final List<Runnable> onHint = new ArrayList<>();

	public GeoguessrPanel()
	{
		setLayout(new BorderLayout(0, 6));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		add(buildTopPanel(), BorderLayout.NORTH);
		add(buildCenterPanel(), BorderLayout.CENTER);
		add(buildHistoryPanel(), BorderLayout.SOUTH);

		setIdle();
	}

	// -------------------------------------------------------------------------
	// Build helpers
	// -------------------------------------------------------------------------

	private JPanel buildTopPanel()
	{
		JPanel top = new JPanel(new BorderLayout(0, 6));
		top.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel title = new JLabel("GeoGuessr RS", SwingConstants.CENTER);
		title.setFont(new Font("Arial", Font.BOLD, 14));
		title.setForeground(Color.WHITE);
		top.add(title, BorderLayout.NORTH);

		JPanel selectors = new JPanel(new GridLayout(2, 1, 0, 4));
		selectors.setBackground(ColorScheme.DARK_GRAY_COLOR);
		selectors.add(buildToggleRow("Mode", GameMode.values(), modeButtons,
			i -> {
				selectedMode = GameMode.values()[i];
			}));
		selectors.add(buildToggleRow("Diff", Difficulty.values(), difficultyButtons,
			i -> {
				selectedDifficulty = Difficulty.values()[i];
			}));

		// Pre-select defaults
		selectToggle(modeButtons, selectedMode.ordinal());
		selectToggle(difficultyButtons, selectedDifficulty.ordinal());

		startButton.setBackground(new Color(0x3C8F3C));
		startButton.setForeground(Color.WHITE);
		startButton.setFocusPainted(false);
		startButton.addActionListener(e ->
		{
			if (onStartRound != null)
			{
				onStartRound.run();
			}
		});

		guessButton.setBackground(new Color(0xC07000));
		guessButton.setForeground(Color.WHITE);
		guessButton.setFocusPainted(false);
		guessButton.setVisible(false);
		guessButton.addActionListener(e ->
		{
			if (onGuess != null)
			{
				onGuess.run();
			}
		});

		JPanel actionButtons = new JPanel(new GridLayout(2, 1, 0, 4));
		actionButtons.setBackground(ColorScheme.DARK_GRAY_COLOR);
		actionButtons.add(startButton);
		actionButtons.add(guessButton);

		JPanel middle = new JPanel(new BorderLayout(0, 4));
		middle.setBackground(ColorScheme.DARK_GRAY_COLOR);
		middle.add(selectors, BorderLayout.NORTH);
		middle.add(actionButtons, BorderLayout.SOUTH);
		top.add(middle, BorderLayout.SOUTH);

		return top;
	}

	/** Builds a labelled row of mutually exclusive toggle buttons. */
	private JPanel buildToggleRow(String label, Object[] values, JButton[] buttons, java.util.function.IntConsumer onSelect)
	{
		JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel lbl = new JLabel(label + ":");
		lbl.setForeground(Color.GRAY);
		lbl.setFont(new Font("Arial", Font.PLAIN, 11));
		lbl.setPreferredSize(new Dimension(30, 0));
		row.add(lbl, BorderLayout.WEST);

		JPanel btnPanel = new JPanel(new GridLayout(1, values.length, 2, 0));
		btnPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		for (int i = 0; i < values.length; i++)
		{
			final int idx = i;
			String text = values[i].toString();
			// abbreviate for space: "RANDOM"→"Rand", "MEDIUM"→"Med", etc.
			if (text.length() > 4)
			{
				text = text.substring(0, 1).toUpperCase() + text.substring(1, 4).toLowerCase();
			}
			else
			{
				text = text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
			}
			buttons[i] = new JButton(text);
			buttons[i].setForeground(Color.WHITE);
			buttons[i].setFocusPainted(false);
			buttons[i].setFont(new Font("Arial", Font.PLAIN, 11));
			buttons[i].setBackground(TOGGLE_INACTIVE_BG);
			buttons[i].setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
			buttons[i].addActionListener(e ->
			{
				selectToggle(buttons, idx);
				onSelect.accept(idx);
			});
			btnPanel.add(buttons[i]);
		}
		row.add(btnPanel, BorderLayout.CENTER);
		return row;
	}

	private void selectToggle(JButton[] buttons, int selectedIdx)
	{
		for (int i = 0; i < buttons.length; i++)
		{
			buttons[i].setBackground(i == selectedIdx ? TOGGLE_ACTIVE_BG : TOGGLE_INACTIVE_BG);
		}
	}

	private JPanel buildCenterPanel()
	{
		JPanel center = new JPanel(new BorderLayout(0, 6));
		center.setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Clue image
		clueImageLabel.setPreferredSize(new Dimension(CLUE_IMAGE_SIZE, CLUE_IMAGE_SIZE));
		clueImageLabel.setHorizontalAlignment(SwingConstants.CENTER);
		clueImageLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		clueImageLabel.setOpaque(true);
		center.add(clueImageLabel, BorderLayout.NORTH);

		// Hint buttons
		JPanel hintButtonsPanel = new JPanel(new GridLayout(1, 3, 4, 0));
		hintButtonsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		for (int i = 0; i < hintButtons.length; i++)
		{
			final int idx = i;
			hintButtons[i] = new JButton("Hint " + (i + 1));
			hintButtons[i].setBackground(HINT_BUTTON_COLOR);
			hintButtons[i].setForeground(Color.WHITE);
			hintButtons[i].setFocusPainted(false);
			hintButtons[i].setEnabled(false);
			hintButtons[i].addActionListener(e ->
			{
				if (idx < onHint.size() && onHint.get(idx) != null)
				{
					onHint.get(idx).run();
				}
			});
			hintButtonsPanel.add(hintButtons[i]);
		}
		center.add(hintButtonsPanel, BorderLayout.CENTER);

		// Hint text area
		hintText.setEditable(false);
		hintText.setLineWrap(true);
		hintText.setWrapStyleWord(true);
		hintText.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		hintText.setForeground(Color.LIGHT_GRAY);
		hintText.setFont(new Font("Arial", Font.PLAIN, 11));
		hintText.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		hintText.setRows(3);
		center.add(hintText, BorderLayout.SOUTH);

		return center;
	}

	private JPanel buildHistoryPanel()
	{
		JPanel bottom = new JPanel(new BorderLayout(0, 4));
		bottom.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel scores = new JPanel(new GridLayout(1, 2));
		scores.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scoreLabel.setForeground(Color.WHITE);
		scoreLabel.setFont(new Font("Arial", Font.BOLD, 12));
		totalScoreLabel.setForeground(new Color(0xFFD700));
		totalScoreLabel.setFont(new Font("Arial", Font.BOLD, 12));
		scores.add(scoreLabel);
		scores.add(totalScoreLabel);
		bottom.add(scores, BorderLayout.NORTH);

		historyPanel.setLayout(new GridLayout(0, 1, 0, 2));
		historyPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		bottom.add(historyPanel, BorderLayout.CENTER);

		return bottom;
	}

	// -------------------------------------------------------------------------
	// Public API called by GeoguessrPlugin
	// -------------------------------------------------------------------------

	public GameMode getSelectedGameMode()
	{
		return selectedMode;
	}

	public Difficulty getSelectedDifficulty()
	{
		return selectedDifficulty;
	}

	public void setCallbacks(Runnable onStart, List<Runnable> hintCallbacks, Runnable onGuess)
	{
		this.onStartRound = onStart;
		this.onGuess = onGuess;
		this.onHint.clear();
		this.onHint.addAll(hintCallbacks);
	}

	public void setIdle()
	{
		SwingUtilities.invokeLater(() ->
		{
			startButton.setEnabled(true);
			startButton.setText("Start Round");
			guessButton.setVisible(false);
			clueImageLabel.setIcon(null);
			clueImageLabel.setText("Press Start Round");
			clueImageLabel.setForeground(Color.GRAY);
			hintText.setText("");
			for (JButton btn : hintButtons)
			{
				btn.setEnabled(false);
			}
			scoreLabel.setText("Score: —");
			setSelectorsEnabled(true);
		});
	}

	private void setSelectorsEnabled(boolean enabled)
	{
		for (JButton btn : modeButtons)       btn.setEnabled(enabled);
		for (JButton btn : difficultyButtons) btn.setEnabled(enabled);
	}

	public void startRound(Round round, int maxHints, boolean huntMode)
	{
		SwingUtilities.invokeLater(() ->
		{
			startButton.setEnabled(false);
			startButton.setText("Round Active");
			setSelectorsEnabled(false);
			guessButton.setVisible(huntMode);
			guessButton.setEnabled(true);
			hintText.setText("");
			scoreLabel.setText("Score: —");

			BufferedImage img = round.getClueImage();
			if (img != null)
			{
				clueImageLabel.setIcon(new ImageIcon(img.getScaledInstance(CLUE_IMAGE_SIZE, CLUE_IMAGE_SIZE, BufferedImage.SCALE_SMOOTH)));
				clueImageLabel.setText("");
			}
			else
			{
				clueImageLabel.setIcon(null);
				clueImageLabel.setText("No image");
			}

			for (int i = 0; i < hintButtons.length; i++)
			{
				hintButtons[i].setEnabled(i < maxHints);
				hintButtons[i].setText("Hint " + (i + 1));
			}
		});
	}

	public void showHint(int index, String text)
	{
		SwingUtilities.invokeLater(() ->
		{
			if (index < hintButtons.length)
			{
				hintButtons[index].setEnabled(false);
				hintButtons[index].setText("✓ " + (index + 1));
			}
			hintText.setText(hintText.getText().isBlank()
				? text
				: hintText.getText() + "\n" + text);
		});
	}

	public void showResult(RoundResult result)
	{
		totalScore += result.getScore();

		SwingUtilities.invokeLater(() ->
		{
			startButton.setEnabled(true);
			startButton.setText("Start Round");
			guessButton.setVisible(false);
			scoreLabel.setText("Score: " + result.getScore());
			totalScoreLabel.setText("Total: " + totalScore);
			for (JButton btn : hintButtons)
			{
				btn.setEnabled(false);
			}

			// Add to history
			String entry = result.getLocationName() + " — " + result.getScore() + " pts";
			history.addFirst(entry);
			while (history.size() > MAX_HISTORY)
			{
				history.removeLast();
			}
			rebuildHistory();
		});
	}

	private void rebuildHistory()
	{
		historyPanel.removeAll();
		for (String entry : history)
		{
			JLabel lbl = new JLabel(entry);
			lbl.setForeground(Color.LIGHT_GRAY);
			lbl.setFont(new Font("Arial", Font.PLAIN, 11));
			lbl.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
			historyPanel.add(lbl);
		}
		historyPanel.revalidate();
		historyPanel.repaint();
	}
}
