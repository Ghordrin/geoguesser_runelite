package com.geoguessrrs;

import com.geoguessrrs.DailyStore.DailyAttempt;
import com.geoguessrrs.round.Round;
import com.geoguessrrs.round.RoundResult;
import com.geoguessrrs.scores.PersonalBestEntry;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

public class GeoguessrPanel extends PluginPanel
{
	private static final int   CLUE_IMAGE_SIZE   = 200;
	private static final Color HINT_BUTTON_COLOR = new Color(0x4A90D9);
	private static final Color GOLD              = new Color(0xFFD700);

	// Top controls
	private final JButton startButton = new JButton("Start Today's Challenge");
	private final JButton guessButton = new JButton("Guess Here!");

	// Daily header
	private final JLabel attemptsLabel = new JLabel("", SwingConstants.CENTER);

	// Clue image
	private final JLabel clueImageLabel = new JLabel();

	// Hint section
	private final JButton[] hintButtons = new JButton[3];

	// Result feedback
	private final JLabel scoreLabel = new JLabel("", SwingConstants.CENTER);
	private final JLabel pbLabel    = new JLabel("", SwingConstants.CENTER);

	// Today's attempt log
	private final JPanel attemptLogPanel = new JPanel();

	// Personal bests (collapsible)
	private boolean  pbExpanded     = false;
	private final JPanel  pbRowsPanel    = new JPanel();
	private final JButton pbToggleButton = new JButton("Personal Bests ▼");

	// Callbacks wired by GeoguessrPlugin
	private Runnable             onStartRound;
	private Runnable             onGuess;
	private Runnable             onDebugReset; // non-null only in dev mode
	private final List<Runnable> onHint = new ArrayList<>();

	public GeoguessrPanel()
	{
		setLayout(new BorderLayout(0, 6));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		add(buildTopPanel(),    BorderLayout.NORTH);
		add(buildCenterPanel(), BorderLayout.CENTER);

		JPanel south = new JPanel(new BorderLayout(0, 4));
		south.setBackground(ColorScheme.DARK_GRAY_COLOR);
		south.add(buildAttemptLogPanel(),    BorderLayout.NORTH);
		south.add(buildPersonalBestsPanel(), BorderLayout.CENTER);
		if (com.geoguessrrs.DevMode.isEnabled())
		{
			south.add(buildDebugPanel(), BorderLayout.SOUTH);
		}
		add(south, BorderLayout.SOUTH);
	}

	// -------------------------------------------------------------------------
	// Build helpers
	// -------------------------------------------------------------------------

	private JPanel buildTopPanel()
	{
		JPanel top = new JPanel(new BorderLayout(0, 6));
		top.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel title = new JLabel("Daily Challenge", SwingConstants.CENTER);
		title.setFont(new Font("Arial", Font.BOLD, 14));
		title.setForeground(Color.WHITE);
		top.add(title, BorderLayout.NORTH);

		attemptsLabel.setFont(new Font("Arial", Font.PLAIN, 13));
		attemptsLabel.setForeground(GOLD);
		top.add(attemptsLabel, BorderLayout.CENTER);

		JPanel buttons = new JPanel(new GridLayout(2, 1, 0, 4));
		buttons.setBackground(ColorScheme.DARK_GRAY_COLOR);

		startButton.setBackground(new Color(0x3C8F3C));
		startButton.setForeground(Color.WHITE);
		startButton.setFocusPainted(false);
		startButton.addActionListener(e -> { if (onStartRound != null) onStartRound.run(); });
		buttons.add(startButton);

		guessButton.setBackground(new Color(0xC07000));
		guessButton.setForeground(Color.WHITE);
		guessButton.setFocusPainted(false);
		guessButton.setVisible(false);
		guessButton.addActionListener(e -> { if (onGuess != null) onGuess.run(); });
		buttons.add(guessButton);

		top.add(buttons, BorderLayout.SOUTH);
		return top;
	}

	private JPanel buildCenterPanel()
	{
		JPanel center = new JPanel(new BorderLayout(0, 6));
		center.setBackground(ColorScheme.DARK_GRAY_COLOR);

		clueImageLabel.setPreferredSize(new Dimension(CLUE_IMAGE_SIZE, CLUE_IMAGE_SIZE));
		clueImageLabel.setHorizontalAlignment(SwingConstants.CENTER);
		clueImageLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		clueImageLabel.setOpaque(true);
		center.add(clueImageLabel, BorderLayout.NORTH);

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
				if (idx < onHint.size() && onHint.get(idx) != null) onHint.get(idx).run();
			});
			hintButtonsPanel.add(hintButtons[i]);
		}
		center.add(hintButtonsPanel, BorderLayout.CENTER);

		JPanel feedback = new JPanel(new GridLayout(2, 1, 0, 2));
		feedback.setBackground(ColorScheme.DARK_GRAY_COLOR);

		scoreLabel.setFont(new Font("Arial", Font.BOLD, 12));
		scoreLabel.setForeground(Color.WHITE);
		pbLabel.setFont(new Font("Arial", Font.BOLD, 11));
		pbLabel.setForeground(GOLD);

		feedback.add(scoreLabel);
		feedback.add(pbLabel);

		center.add(feedback, BorderLayout.SOUTH);
		return center;
	}

	private JPanel buildAttemptLogPanel()
	{
		JPanel wrapper = new JPanel(new BorderLayout(0, 2));
		wrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel header = new JLabel("Today's Attempts");
		header.setForeground(Color.GRAY);
		header.setFont(new Font("Arial", Font.PLAIN, 11));
		wrapper.add(header, BorderLayout.NORTH);

		attemptLogPanel.setLayout(new GridLayout(0, 1, 0, 2));
		attemptLogPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		wrapper.add(attemptLogPanel, BorderLayout.CENTER);

		return wrapper;
	}

	private JPanel buildPersonalBestsPanel()
	{
		JPanel container = new JPanel(new BorderLayout(0, 2));
		container.setBackground(ColorScheme.DARK_GRAY_COLOR);

		pbToggleButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		pbToggleButton.setForeground(Color.LIGHT_GRAY);
		pbToggleButton.setFont(new Font("Arial", Font.PLAIN, 11));
		pbToggleButton.setFocusPainted(false);
		pbToggleButton.setBorder(BorderFactory.createEmptyBorder(3, 4, 3, 4));
		pbToggleButton.addActionListener(e ->
		{
			pbExpanded = !pbExpanded;
			pbRowsPanel.setVisible(pbExpanded);
			pbToggleButton.setText(pbExpanded ? "Personal Bests ▲" : "Personal Bests ▼");
		});
		container.add(pbToggleButton, BorderLayout.NORTH);

		pbRowsPanel.setLayout(new GridLayout(0, 1, 0, 1));
		pbRowsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		pbRowsPanel.setVisible(false);
		container.add(pbRowsPanel, BorderLayout.CENTER);

		return container;
	}

	// -------------------------------------------------------------------------
	// Public API called by GeoguessrPlugin
	// -------------------------------------------------------------------------

	private JPanel buildDebugPanel()
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		JButton resetBtn = new JButton("[DEV] Reset Daily");
		resetBtn.setBackground(new Color(0x8B0000));
		resetBtn.setForeground(Color.WHITE);
		resetBtn.setFocusPainted(false);
		resetBtn.setFont(new Font("Arial", Font.PLAIN, 11));
		resetBtn.addActionListener(e -> { if (onDebugReset != null) onDebugReset.run(); });
		panel.add(resetBtn, BorderLayout.CENTER);
		return panel;
	}

	public void setCallbacks(Runnable onStart, List<Runnable> hintCallbacks, Runnable onGuess, Runnable onDebugReset)
	{
		this.onStartRound  = onStart;
		this.onGuess       = onGuess;
		this.onDebugReset  = onDebugReset;
		this.onHint.clear();
		this.onHint.addAll(hintCallbacks);
	}

	/**
	 * Refreshes idle-state UI based on daily progress.
	 * Called on startup, after logout, and after each attempt.
	 */
	public void updateDailyState(int attemptsUsed, boolean exhausted, List<DailyAttempt> attempts)
	{
		SwingUtilities.invokeLater(() ->
		{
			// Attempt dots: ●●○○
			StringBuilder dots = new StringBuilder();
			for (int i = 0; i < DailyStore.MAX_ATTEMPTS; i++)
			{
				dots.append(i < attemptsUsed ? "●" : "○");
				if (i < DailyStore.MAX_ATTEMPTS - 1) dots.append(' ');
			}
			attemptsLabel.setText(dots.toString());

			if (exhausted)
			{
				startButton.setText("Come back tomorrow!");
				startButton.setBackground(new Color(0x555555));
				startButton.setEnabled(false);
			}
			else if (attemptsUsed == 0)
			{
				startButton.setText("Start Today's Challenge");
				startButton.setBackground(new Color(0x3C8F3C));
				startButton.setEnabled(true);
			}
			else
			{
				int remaining = DailyStore.MAX_ATTEMPTS - attemptsUsed;
				startButton.setText("Next Location  (" + remaining + " left)");
				startButton.setBackground(new Color(0x3C8F3C));
				startButton.setEnabled(true);
			}

			guessButton.setVisible(false);
			clueImageLabel.setIcon(null);
			clueImageLabel.setText(exhausted ? "See you tomorrow!" : "Press start to begin");
			clueImageLabel.setForeground(Color.GRAY);
			for (JButton btn : hintButtons) btn.setEnabled(false);

			rebuildAttemptLog(attempts);
		});
	}

	public void startRound(Round round, int maxHints, boolean huntMode)
	{
		SwingUtilities.invokeLater(() ->
		{
			startButton.setEnabled(false);
			startButton.setText("Round Active");
			guessButton.setVisible(huntMode);
			guessButton.setEnabled(true);
			scoreLabel.setText("");
			pbLabel.setText("");

			BufferedImage img = round.getClueImage();
			if (img != null)
			{
				clueImageLabel.setIcon(new ImageIcon(
					img.getScaledInstance(CLUE_IMAGE_SIZE, CLUE_IMAGE_SIZE, BufferedImage.SCALE_SMOOTH)));
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

	public void showHint(int index, BufferedImage image)
	{
		SwingUtilities.invokeLater(() ->
		{
			if (index < hintButtons.length)
			{
				hintButtons[index].setEnabled(false);
				hintButtons[index].setText("+" + (index + 1));
			}
			if (image != null)
			{
				clueImageLabel.setIcon(new ImageIcon(
					image.getScaledInstance(CLUE_IMAGE_SIZE, CLUE_IMAGE_SIZE, BufferedImage.SCALE_SMOOTH)));
			}
		});
	}

	public void showResult(RoundResult result, boolean isNewPb)
	{
		SwingUtilities.invokeLater(() ->
		{
			guessButton.setVisible(false);
			scoreLabel.setText("Score: " + result.getScore() + "  ·  " + result.getDistance() + " tiles away");
			scoreLabel.setForeground(Color.WHITE);

			if (isNewPb)
			{
				pbLabel.setText("★ NEW PERSONAL BEST!");
				pbLabel.setForeground(GOLD);
			}
			else
			{
				pbLabel.setText("");
			}
			pbLabel.setVisible(true);

			for (JButton btn : hintButtons) btn.setEnabled(false);
		});
	}

	public void updatePersonalBests(List<PersonalBestEntry> entries)
	{
		SwingUtilities.invokeLater(() ->
		{
			pbRowsPanel.removeAll();
			int shown = Math.min(entries.size(), 10);
			for (int i = 0; i < shown; i++)
			{
				PersonalBestEntry e = entries.get(i);
				String text = (i + 1) + ". " + e.getLocationName()
					+ "  —  " + e.getBestScore() + " pts  (" + e.getBestDistance() + " tiles)";
				JLabel row = new JLabel(text);
				row.setForeground(Color.LIGHT_GRAY);
				row.setFont(new Font("Arial", Font.PLAIN, 11));
				row.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 4));
				pbRowsPanel.add(row);
			}
			if (entries.isEmpty())
			{
				JLabel empty = new JLabel("No personal bests yet.");
				empty.setForeground(Color.GRAY);
				empty.setFont(new Font("Arial", Font.ITALIC, 11));
				empty.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 4));
				pbRowsPanel.add(empty);
			}
			pbRowsPanel.revalidate();
			pbRowsPanel.repaint();
		});
	}

	// -------------------------------------------------------------------------
	// Private helpers
	// -------------------------------------------------------------------------

	private void rebuildAttemptLog(List<DailyAttempt> attempts)
	{
		attemptLogPanel.removeAll();
		for (int i = 0; i < attempts.size(); i++)
		{
			DailyAttempt a = attempts.get(i);
			String text = "#" + (i + 1) + "  " + a.getLocationName() + "  —  "
				+ a.getScore() + " pts  ·  " + a.getDistance() + " tiles";
			JLabel row = new JLabel(text);
			row.setForeground(scoreColor(a.getScore()));
			row.setFont(new Font("Arial", Font.PLAIN, 11));
			row.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 4));
			attemptLogPanel.add(row);
		}
		if (attempts.isEmpty())
		{
			JLabel empty = new JLabel("No attempts yet today.");
			empty.setForeground(Color.GRAY);
			empty.setFont(new Font("Arial", Font.ITALIC, 11));
			empty.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 4));
			attemptLogPanel.add(empty);
		}
		attemptLogPanel.revalidate();
		attemptLogPanel.repaint();
	}

	private static Color scoreColor(int score)
	{
		if (score >= 3000) return new Color(0x55DD55);
		if (score >= 1500) return new Color(0xFFD700);
		return new Color(0xFF7070);
	}
}
