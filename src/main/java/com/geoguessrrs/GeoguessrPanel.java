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
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.MatteBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

public class GeoguessrPanel extends PluginPanel
{
	// ── Colours ───────────────────────────────────────────────────────────────
	private static final Color GOLD         = new Color(0xFFD700);
	private static final Color GREEN        = new Color(0x4CAF50);
	private static final Color BLUE         = new Color(0x3A7DC9);
	private static final Color SECTION_BG   = ColorScheme.DARKER_GRAY_COLOR;
	private static final Color DIVIDER      = new Color(0x3A3A3A);

	// ── Image ────────────────────────────────────────────────────────────────
	private static final int IMAGE_SIZE = 196;

	// ── Challenge header ─────────────────────────────────────────────────────
	private final JLabel  attemptsLabel = new JLabel("○ ○ ○ ○", SwingConstants.CENTER);
	private final JButton startButton   = new JButton("Start Today's Challenge");

	// ── Clue + hints ─────────────────────────────────────────────────────────
	private final JLabel    clueImageLabel  = new JLabel("", SwingConstants.CENTER);
	private final JButton[] hintButtons     = new JButton[3];
	private final JButton   guessButton     = new JButton("Guess Here!");
	private int             activeMaxHints  = 0;

	// ── Result card ──────────────────────────────────────────────────────────
	private final JPanel resultCard    = new JPanel();
	private final JLabel resultScore   = new JLabel("", SwingConstants.CENTER);
	private final JLabel resultDetail  = new JLabel("", SwingConstants.CENTER);
	private final JLabel resultPb      = new JLabel("", SwingConstants.CENTER);

	// ── Attempt log ──────────────────────────────────────────────────────────
	private final JPanel attemptLogPanel = new JPanel();

	// ── Leaderboard ──────────────────────────────────────────────────────────
	private final JPanel  lbRowsPanel  = new JPanel();
	private final JButton lbRefreshBtn = new JButton("↻");

	// ── Personal bests ───────────────────────────────────────────────────────
	private boolean pbExpanded = false;
	private final JPanel  pbRowsPanel    = new JPanel();
	private final JButton pbToggleButton = new JButton("Personal Bests ▼");

	// ── Callbacks ────────────────────────────────────────────────────────────
	private Runnable             onStartRound;
	private Runnable             onGuess;
	private Runnable             onDebugReset;
	private Runnable             onRefreshLeaderboard;
	private final List<Runnable> onHint = new ArrayList<>();

	// =========================================================================
	// Constructor
	// =========================================================================

	public GeoguessrPanel()
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		add(buildChallengeHeader());
		add(Box.createVerticalStrut(6));
		add(buildClueSection());
		add(Box.createVerticalStrut(6));
		add(buildResultCard());
		add(Box.createVerticalStrut(6));
		add(buildAttemptLog());
		add(Box.createVerticalStrut(6));
		add(buildLeaderboard());
		add(Box.createVerticalStrut(6));
		add(buildPersonalBests());

		if (DevMode.isEnabled())
		{
			add(Box.createVerticalStrut(6));
			add(buildDebugSection());
		}
	}

	// =========================================================================
	// Section builders
	// =========================================================================

	private JPanel buildChallengeHeader()
	{
		JPanel card = card();

		JLabel title = sectionLabel("DAILY CHALLENGE");
		card.add(title);
		card.add(Box.createVerticalStrut(6));

		attemptsLabel.setFont(new Font("Arial", Font.PLAIN, 16));
		attemptsLabel.setForeground(GOLD);
		attemptsLabel.setAlignmentX(CENTER_ALIGNMENT);
		card.add(attemptsLabel);
		card.add(Box.createVerticalStrut(8));

		styleActionButton(startButton, GREEN);
		startButton.addActionListener(e -> { if (onStartRound != null) onStartRound.run(); });
		card.add(startButton);

		return card;
	}

	private JPanel buildClueSection()
	{
		JPanel card = card();

		// Image container
		JPanel imageContainer = new JPanel(new BorderLayout());
		imageContainer.setBackground(new Color(0x1A1A1A));
		imageContainer.setBorder(BorderFactory.createLineBorder(DIVIDER, 1));
		imageContainer.setAlignmentX(CENTER_ALIGNMENT);
		int size = IMAGE_SIZE;
		imageContainer.setPreferredSize(new Dimension(size, size));
		imageContainer.setMinimumSize(new Dimension(size, size));
		imageContainer.setMaximumSize(new Dimension(size, size));

		clueImageLabel.setHorizontalAlignment(SwingConstants.CENTER);
		clueImageLabel.setVerticalAlignment(SwingConstants.CENTER);
		clueImageLabel.setForeground(new Color(0x666666));
		clueImageLabel.setFont(new Font("Arial", Font.ITALIC, 12));
		clueImageLabel.setText("Press start to begin");
		imageContainer.add(clueImageLabel, BorderLayout.CENTER);
		card.add(imageContainer);

		card.add(Box.createVerticalStrut(6));

		// Hint buttons
		JPanel hintRow = new JPanel(new GridLayout(1, 3, 4, 0));
		hintRow.setBackground(SECTION_BG);
		hintRow.setAlignmentX(CENTER_ALIGNMENT);
		hintRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

		for (int i = 0; i < hintButtons.length; i++)
		{
			final int idx = i;
			hintButtons[i] = new JButton("+" + (i + 1));
			styleHintButton(hintButtons[i], false);
			hintButtons[i].addActionListener(e ->
			{
				if (idx < onHint.size() && onHint.get(idx) != null) onHint.get(idx).run();
			});
			hintRow.add(hintButtons[i]);
		}
		card.add(hintRow);

		card.add(Box.createVerticalStrut(6));

		// Guess button
		styleActionButton(guessButton, new Color(0xB8860B));
		guessButton.setVisible(false);
		guessButton.addActionListener(e -> { if (onGuess != null) onGuess.run(); });
		card.add(guessButton);

		return card;
	}

	private JPanel buildResultCard()
	{
		resultCard.setLayout(new BoxLayout(resultCard, BoxLayout.Y_AXIS));
		resultCard.setBackground(new Color(0x252525));
		resultCard.setBorder(BorderFactory.createCompoundBorder(
			new MatteBorder(1, 0, 1, 0, DIVIDER),
			BorderFactory.createEmptyBorder(8, 8, 8, 8)
		));
		resultCard.setAlignmentX(CENTER_ALIGNMENT);
		resultCard.setVisible(false);

		resultScore.setFont(new Font("Arial", Font.BOLD, 22));
		resultScore.setForeground(Color.WHITE);
		resultScore.setAlignmentX(CENTER_ALIGNMENT);

		resultDetail.setFont(new Font("Arial", Font.PLAIN, 11));
		resultDetail.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		resultDetail.setAlignmentX(CENTER_ALIGNMENT);

		resultPb.setFont(new Font("Arial", Font.BOLD, 11));
		resultPb.setForeground(GOLD);
		resultPb.setAlignmentX(CENTER_ALIGNMENT);

		resultCard.add(resultScore);
		resultCard.add(Box.createVerticalStrut(3));
		resultCard.add(resultDetail);
		resultCard.add(resultPb);

		return resultCard;
	}

	private JPanel buildAttemptLog()
	{
		JPanel wrapper = new JPanel();
		wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
		wrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		wrapper.setAlignmentX(CENTER_ALIGNMENT);

		wrapper.add(sectionLabel("TODAY'S ATTEMPTS"));
		wrapper.add(Box.createVerticalStrut(4));

		attemptLogPanel.setLayout(new BoxLayout(attemptLogPanel, BoxLayout.Y_AXIS));
		attemptLogPanel.setBackground(SECTION_BG);
		attemptLogPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
		attemptLogPanel.setAlignmentX(CENTER_ALIGNMENT);

		JLabel empty = emptyLabel("No attempts yet today.");
		attemptLogPanel.add(empty);
		wrapper.add(attemptLogPanel);

		return wrapper;
	}

	private JPanel buildLeaderboard()
	{
		JPanel wrapper = new JPanel();
		wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
		wrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		wrapper.setAlignmentX(CENTER_ALIGNMENT);

		// Header row
		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(ColorScheme.DARK_GRAY_COLOR);
		header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

		JLabel title = sectionLabel("DAILY LEADERBOARD");
		title.setBorder(null);
		header.add(title, BorderLayout.WEST);

		lbRefreshBtn.setFont(new Font("Arial", Font.PLAIN, 10));
		lbRefreshBtn.setBackground(SECTION_BG);
		lbRefreshBtn.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		lbRefreshBtn.setBorderPainted(false);
		lbRefreshBtn.setFocusPainted(false);
		lbRefreshBtn.setContentAreaFilled(false);
		lbRefreshBtn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
		lbRefreshBtn.setToolTipText("Refresh leaderboard");
		lbRefreshBtn.addActionListener(e -> { if (onRefreshLeaderboard != null) onRefreshLeaderboard.run(); });
		header.add(lbRefreshBtn, BorderLayout.EAST);

		wrapper.add(header);
		wrapper.add(Box.createVerticalStrut(4));

		lbRowsPanel.setLayout(new BoxLayout(lbRowsPanel, BoxLayout.Y_AXIS));
		lbRowsPanel.setBackground(SECTION_BG);
		lbRowsPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
		lbRowsPanel.setAlignmentX(CENTER_ALIGNMENT);

		lbRowsPanel.add(emptyLabel("Connecting to server…"));
		wrapper.add(lbRowsPanel);

		return wrapper;
	}

	private JPanel buildPersonalBests()
	{
		JPanel wrapper = new JPanel();
		wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
		wrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		wrapper.setAlignmentX(CENTER_ALIGNMENT);

		pbToggleButton.setBackground(SECTION_BG);
		pbToggleButton.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		pbToggleButton.setFont(new Font("Arial", Font.BOLD, 11));
		pbToggleButton.setFocusPainted(false);
		pbToggleButton.setBorderPainted(false);
		pbToggleButton.setContentAreaFilled(false);
		pbToggleButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
		pbToggleButton.setAlignmentX(LEFT_ALIGNMENT);
		pbToggleButton.addActionListener(e ->
		{
			pbExpanded = !pbExpanded;
			pbRowsPanel.setVisible(pbExpanded);
			pbToggleButton.setText(pbExpanded ? "Personal Bests ▲" : "Personal Bests ▼");
		});
		wrapper.add(pbToggleButton);

		pbRowsPanel.setLayout(new BoxLayout(pbRowsPanel, BoxLayout.Y_AXIS));
		pbRowsPanel.setBackground(SECTION_BG);
		pbRowsPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
		pbRowsPanel.setVisible(false);
		pbRowsPanel.setAlignmentX(CENTER_ALIGNMENT);
		pbRowsPanel.add(emptyLabel("No personal bests yet."));
		wrapper.add(pbRowsPanel);

		return wrapper;
	}

	private JPanel buildDebugSection()
	{
		JPanel card = card();
		JButton resetBtn = new JButton("[DEV] Reset Daily");
		resetBtn.setBackground(new Color(0x8B0000));
		resetBtn.setForeground(Color.WHITE);
		resetBtn.setFocusPainted(false);
		resetBtn.setFont(new Font("Arial", Font.PLAIN, 11));
		resetBtn.setAlignmentX(CENTER_ALIGNMENT);
		resetBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
		resetBtn.addActionListener(e -> { if (onDebugReset != null) onDebugReset.run(); });
		card.add(resetBtn);
		return card;
	}

	// =========================================================================
	// Public API — called by GeoguessrPlugin
	// =========================================================================

	public void setCallbacks(Runnable onStart, List<Runnable> hintCallbacks, Runnable onGuess,
		Runnable onDebugReset, Runnable onRefreshLeaderboard)
	{
		this.onStartRound         = onStart;
		this.onGuess              = onGuess;
		this.onDebugReset         = onDebugReset;
		this.onRefreshLeaderboard = onRefreshLeaderboard;
		this.onHint.clear();
		this.onHint.addAll(hintCallbacks);
	}

	public void updateDailyState(int attemptsUsed, boolean exhausted, List<DailyAttempt> attempts)
	{
		SwingUtilities.invokeLater(() ->
		{
			// Attempt dots
			StringBuilder dots = new StringBuilder();
			for (int i = 0; i < DailyStore.MAX_ATTEMPTS; i++)
			{
				dots.append(i < attemptsUsed ? "●" : "○");
				if (i < DailyStore.MAX_ATTEMPTS - 1) dots.append("  ");
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
				startButton.setBackground(GREEN);
				startButton.setEnabled(true);
			}
			else
			{
				int remaining = DailyStore.MAX_ATTEMPTS - attemptsUsed;
				startButton.setText("Next Location  (" + remaining + " left)");
				startButton.setBackground(GREEN);
				startButton.setEnabled(true);
			}

			guessButton.setVisible(false);
			clueImageLabel.setIcon(null);
			clueImageLabel.setText(exhausted ? "See you tomorrow!" : "Press start to begin");
			for (JButton btn : hintButtons) resetHintButton(btn);
			resultCard.setVisible(false);

			rebuildAttemptLog(attempts);
			revalidate();
			repaint();
		});
	}

	public void startRound(Round round, int maxHints)
	{
		SwingUtilities.invokeLater(() ->
		{
			startButton.setEnabled(false);
			startButton.setText("Round Active");
			guessButton.setVisible(true);
			guessButton.setEnabled(true);
			resultCard.setVisible(false);

			BufferedImage img = round.getClueImage();
			if (img != null)
			{
				clueImageLabel.setIcon(new ImageIcon(
					img.getScaledInstance(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.SCALE_SMOOTH)));
				clueImageLabel.setText("");
			}
			else
			{
				clueImageLabel.setIcon(null);
				clueImageLabel.setText("No preview");
			}

			activeMaxHints = maxHints;
			for (int i = 0; i < hintButtons.length; i++)
			{
				resetHintButton(hintButtons[i]);
				hintButtons[i].setEnabled(i == 0 && maxHints > 0);
				if (i == 0 && maxHints > 0)
				{
					styleHintButton(hintButtons[i], true);
				}
			}

			revalidate();
			repaint();
		});
	}

	public void showHint(int index, BufferedImage image)
	{
		SwingUtilities.invokeLater(() ->
		{
			if (index < hintButtons.length)
			{
				hintButtons[index].setEnabled(false);
				hintButtons[index].setBackground(new Color(0x2A4A2A));
				hintButtons[index].setForeground(new Color(0x66BB66));
				hintButtons[index].setText("✓ +" + (index + 1));

				int next = index + 1;
				if (next < hintButtons.length && next < activeMaxHints)
				{
					styleHintButton(hintButtons[next], true);
					hintButtons[next].setEnabled(true);
				}
			}
			if (image != null)
			{
				clueImageLabel.setIcon(new ImageIcon(
					image.getScaledInstance(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.SCALE_SMOOTH)));
			}
		});
	}

	public void showResult(RoundResult result, boolean isNewPb)
	{
		SwingUtilities.invokeLater(() ->
		{
			guessButton.setVisible(false);
			for (JButton btn : hintButtons) btn.setEnabled(false);

			int score = result.getScore();
			resultScore.setText(String.format("%,d pts", score));
			resultScore.setForeground(scoreColor(score));

			String detail = result.getDistance() + " tiles  ·  " + result.getElapsedSeconds() + "s";
			if (result.getHintsUsed() > 0)
			{
				detail += "  ·  " + result.getHintsUsed() + " hint" + (result.getHintsUsed() > 1 ? "s" : "");
			}
			resultDetail.setText(detail);

			if (isNewPb)
			{
				resultPb.setText("★  New personal best!");
				resultPb.setVisible(true);
			}
			else
			{
				resultPb.setVisible(false);
			}

			resultCard.setVisible(true);
			revalidate();
			repaint();
		});
	}

	public void updateLeaderboard(List<String> top10, String playerRow)
	{
		SwingUtilities.invokeLater(() ->
		{
			lbRowsPanel.removeAll();

			if (top10.isEmpty())
			{
				lbRowsPanel.add(emptyLabel("No scores submitted yet today."));
			}
			else
			{
				for (int i = 0; i < top10.size(); i++)
				{
					JLabel row = lbRow(top10.get(i), i == 0);
					lbRowsPanel.add(row);
				}

				if (playerRow != null)
				{
					JLabel sep = new JLabel("· · ·", SwingConstants.CENTER);
					sep.setForeground(new Color(0x555555));
					sep.setFont(new Font("Arial", Font.PLAIN, 9));
					sep.setBorder(BorderFactory.createEmptyBorder(1, 8, 1, 8));
					sep.setAlignmentX(LEFT_ALIGNMENT);
					lbRowsPanel.add(sep);

					JLabel you = lbRow(playerRow, false);
					you.setForeground(GOLD);
					you.setFont(new Font("Consolas", Font.BOLD, 11));
					lbRowsPanel.add(you);
				}
			}

			lbRowsPanel.revalidate();
			lbRowsPanel.repaint();
		});
	}

	public void updatePersonalBests(List<PersonalBestEntry> entries)
	{
		SwingUtilities.invokeLater(() ->
		{
			pbRowsPanel.removeAll();
			if (entries.isEmpty())
			{
				pbRowsPanel.add(emptyLabel("No personal bests yet."));
			}
			else
			{
				int shown = Math.min(entries.size(), 10);
				for (int i = 0; i < shown; i++)
				{
					PersonalBestEntry e = entries.get(i);
					String text = (i + 1) + ".  " + e.getLocationName()
						+ "  —  " + String.format("%,d", e.getBestScore()) + " pts";
					JLabel row = new JLabel(text);
					row.setForeground(scoreColor(e.getBestScore()));
					row.setFont(new Font("Arial", Font.PLAIN, 11));
					row.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
					row.setAlignmentX(LEFT_ALIGNMENT);
					pbRowsPanel.add(row);
				}
			}
			pbRowsPanel.revalidate();
			pbRowsPanel.repaint();
		});
	}

	// =========================================================================
	// Private helpers
	// =========================================================================

	private void rebuildAttemptLog(List<DailyAttempt> attempts)
	{
		attemptLogPanel.removeAll();
		if (attempts.isEmpty())
		{
			attemptLogPanel.add(emptyLabel("No attempts yet today."));
		}
		else
		{
			for (int i = 0; i < attempts.size(); i++)
			{
				DailyAttempt a = attempts.get(i);
				JPanel row = new JPanel(new BorderLayout(6, 0));
				row.setBackground(i % 2 == 0 ? SECTION_BG : new Color(0x282828));
				row.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
				row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
				row.setAlignmentX(LEFT_ALIGNMENT);

				JLabel name = new JLabel("#" + (i + 1) + "  " + a.getLocationName());
				name.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
				name.setFont(new Font("Arial", Font.PLAIN, 11));

				JLabel score = new JLabel(String.format("%,d", a.getScore()));
				score.setForeground(scoreColor(a.getScore()));
				score.setFont(new Font("Arial", Font.BOLD, 11));
				score.setHorizontalAlignment(SwingConstants.RIGHT);

				row.add(name, BorderLayout.WEST);
				row.add(score, BorderLayout.EAST);
				attemptLogPanel.add(row);
			}
		}
		attemptLogPanel.revalidate();
		attemptLogPanel.repaint();
	}

	private static JPanel card()
	{
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.setBackground(SECTION_BG);
		p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		p.setAlignmentX(CENTER_ALIGNMENT);
		return p;
	}

	private static JLabel sectionLabel(String text)
	{
		JLabel label = new JLabel(text);
		label.setFont(new Font("Arial", Font.BOLD, 10));
		label.setForeground(new Color(0x777777));
		label.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));
		label.setAlignmentX(LEFT_ALIGNMENT);
		return label;
	}

	private static JLabel emptyLabel(String text)
	{
		JLabel label = new JLabel(text);
		label.setForeground(new Color(0x666666));
		label.setFont(new Font("Arial", Font.ITALIC, 11));
		label.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
		label.setAlignmentX(LEFT_ALIGNMENT);
		return label;
	}

	private static JLabel lbRow(String text, boolean gold)
	{
		JLabel label = new JLabel(text);
		label.setForeground(gold ? GOLD : ColorScheme.LIGHT_GRAY_COLOR);
		label.setFont(new Font("Consolas", Font.PLAIN, 11));
		label.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
		label.setAlignmentX(LEFT_ALIGNMENT);
		return label;
	}

	private static void styleActionButton(JButton btn, Color bg)
	{
		btn.setBackground(bg);
		btn.setForeground(Color.WHITE);
		btn.setFocusPainted(false);
		btn.setBorderPainted(false);
		btn.setFont(new Font("Arial", Font.BOLD, 12));
		btn.setAlignmentX(CENTER_ALIGNMENT);
		btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
	}

	private static void styleHintButton(JButton btn, boolean available)
	{
		if (available)
		{
			btn.setBackground(BLUE);
			btn.setForeground(Color.WHITE);
		}
		else
		{
			btn.setBackground(new Color(0x333333));
			btn.setForeground(new Color(0x555555));
		}
		btn.setFocusPainted(false);
		btn.setBorderPainted(false);
		btn.setFont(new Font("Arial", Font.BOLD, 11));
	}

	private static void resetHintButton(JButton btn)
	{
		styleHintButton(btn, false);
		btn.setEnabled(false);
		btn.setText("+" + (btn.getParent() != null
			? java.util.Arrays.asList(btn.getParent().getComponents()).indexOf(btn) + 1
			: "?"));
	}

	private static Color scoreColor(int score)
	{
		if (score >= 3500) return new Color(0x4CAF50);
		if (score >= 2000) return new Color(0x8BC34A);
		if (score >= 1000) return new Color(0xFFD700);
		if (score >= 400)  return new Color(0xFF9800);
		return new Color(0xF44336);
	}
}
