package Windows;

import ColorSeparator.Operations;
import ColorSeparator.TYPE;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class DropDownWindow {
    private JFrame frame;
    private JLabel dropLabel;
    private JSlider offsetSlider;
    private JTextField offsetField;
    private JButton invertButton;
    private JButton coloredButton;
    private JButton rgbaButton;
    private JButton cmykButton;

    private boolean invert = false;
    private boolean colored = false;
    private int offset = 0;
    private TYPE type = TYPE.RGBA;
    private boolean loading = false;
    private final Font defaultFont = UIManager.getDefaults().getFont("Label.font");

    public DropDownWindow() {
        initFrame();
        initDropLabel();
        initControlPanel();
        finalizeFrame();
    }

    private void initFrame() {
        frame = new JFrame("Color Separator");
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
    }

    private void initDropLabel() {
        dropLabel = new JLabel("Drop IMAGE files here", SwingConstants.CENTER);
        dropLabel.setPreferredSize(new Dimension(300, 200));
        dropLabel.setBorder(BorderFactory.createLineBorder(Color.WHITE));
        dropLabel.setForeground(Color.WHITE);
        dropLabel.setOpaque(true);
        dropLabel.setBackground(Color.BLACK);
        dropLabel.setTransferHandler(createTransferHandler());
        frame.add(dropLabel, BorderLayout.CENTER);
    }

    private TransferHandler createTransferHandler() {
        return new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return !loading && support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }
            
            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) return false;
                
                try {
                    List<File> files = (List<File>) support.getTransferable()
                        .getTransferData(DataFlavor.javaFileListFlavor);
                    
                    for (File f : files) {
                        String name = f.getName().toLowerCase();
                        
                        if (!name.endsWith(".png") && !name.endsWith(".jpg") && !name.endsWith(".jpeg")) {
                            showError("Incorrect image format, use: png, jpg or jpeg");
                            
                            return false;
                        }
                    }
                    
                    setLoadingState(true);
                    processFiles(files);
                    
                    return true;
                } catch (UnsupportedFlavorException | IOException e) {
                    e.printStackTrace();
                    
                    return false;
                }
            }
        };
    }

    private void setLoadingState(boolean state) {
        loading = state;
        toggleControls(!state);
        frame.repaint();
    }

    private void toggleControls(boolean enabled) {
        offsetSlider.setEnabled(enabled);
        offsetField.setEnabled(enabled);
        invertButton.setEnabled(enabled);
        coloredButton.setEnabled(enabled);
        rgbaButton.setEnabled(enabled);
        cmykButton.setEnabled(enabled);
    }

    private void processFiles(List<File> files) {
        final int total = files.size();
        dropLabel.setText("LOADING (1/" + total + ")");

        SwingWorker<Void, Integer> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws InterruptedException, InvocationTargetException {
                Operations op = new Operations(offset, invert, type, colored);

                for (int i = 0; i < total; i++) {
                    final File file = files.get(i);
                    final int num = i + 1;
                    
                    try {
                        op.processFile(file.getPath());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        
                        SwingUtilities.invokeAndWait(() -> {
                            showError("Error processing file (" + num + "/" + total + "): " + file.getName());
                        });
                        
                        break;
                    }
                    
                    publish(i + 2);
                }
                
                return null;
            }
            
            @Override
            protected void process(List<Integer> chunks) {
                int done = chunks.get(chunks.size() - 1);
                dropLabel.setText("LOADING (" + done + "/" + total + ")");
            }
            
            @Override
            protected void done() {
                onProcessingComplete();
            }
        };
        
        worker.execute();
    }

    private void onProcessingComplete() {
        dropLabel.setText("Images Generated");
        
        Timer resetTimer = new Timer(1000, e -> {
            dropLabel.setText("Drop IMAGE files here");
            setLoadingState(false);
        });
        
        resetTimer.setRepeats(false);
        resetTimer.start();
    }

    private void initControlPanel() {
        // Slider
        offsetSlider = new JSlider(-255, 255, offset);
        offsetSlider.setMajorTickSpacing(102);
        offsetSlider.setMinorTickSpacing(34);
        offsetSlider.setPaintTicks(true);
        offsetSlider.setPaintLabels(true);
        offsetSlider.setBackground(Color.BLACK);
        offsetSlider.setForeground(Color.WHITE);
        
        offsetSlider.addChangeListener(e -> {
            if (!loading) {
                offset = offsetSlider.getValue();
                offsetField.setText(String.valueOf(offset));
            }
        });

        offsetField = new JTextField(String.valueOf(offset));
        offsetField.setPreferredSize(new Dimension(50, 20));
        offsetField.setBackground(Color.BLACK);
        offsetField.setForeground(Color.WHITE);
        offsetField.setFont(defaultFont);
        offsetField.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        
        offsetField.addActionListener(e -> {
            if (!loading) {
                String txt = offsetField.getText();
                
                if (!txt.isEmpty()) {
                    int maxLen = txt.startsWith("-") ? 4 : 3;
                    txt = txt.substring(0, Math.min(txt.length(), maxLen));
                    int val = Integer.parseInt(txt);
                    val = Math.max(-255, Math.min(255, val));
                    offsetSlider.setValue(val);
                    offsetField.setText(String.valueOf(val));
                } else {
                    offsetField.setText(String.valueOf(offsetSlider.getValue()));
                }
                
                offsetField.transferFocus();
            }
        });

        JPanel sliderPanel = new JPanel(new BorderLayout());
        sliderPanel.setBackground(Color.BLACK);
        JLabel lbl = new JLabel("Contrast", SwingConstants.LEFT);
        lbl.setOpaque(true);
        lbl.setForeground(Color.WHITE);
        lbl.setBackground(Color.BLACK);
        sliderPanel.add(lbl, BorderLayout.NORTH);
        sliderPanel.add(offsetSlider, BorderLayout.CENTER);
        sliderPanel.add(offsetField, BorderLayout.EAST);

        // Invert & Colored toggles
        invertButton  = new JButton("Invert");
        coloredButton = new JButton("Colored");
        
        configureToggleButtons(
            new JButton[]{invertButton, coloredButton},
            new BooleanSupplier[]{() -> invert, () -> colored},
            List.of(
                v -> invert = v,
                v -> colored = v
            )
        );

        // RGBA / CMYK as radio-style buttons
        rgbaButton = new JButton("RGBA");
        cmykButton = new JButton("CMYK");
        styleButton(rgbaButton);
        styleButton(cmykButton);
        
        // default selection
        updateToggleState(rgbaButton, true);
        updateToggleState(cmykButton, false);

        rgbaButton.addActionListener(e -> {
            if (loading || type == TYPE.RGBA) return;
            
            type = TYPE.RGBA;
            updateToggleState(rgbaButton, true);
            updateToggleState(cmykButton, false);
        });
        
        cmykButton.addActionListener(e -> {
            if (loading || type == TYPE.CMYK) return;
            
            type = TYPE.CMYK;
            updateToggleState(cmykButton, true);
            updateToggleState(rgbaButton, false);
        });

        JPanel togglePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        togglePanel.setBackground(Color.BLACK);
        togglePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        togglePanel.add(invertButton);
        togglePanel.add(coloredButton);

        JPanel sidePanel = new JPanel(new GridLayout(0, 1, 0, 5));
        sidePanel.setBackground(Color.BLACK);
        sidePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        sidePanel.add(rgbaButton);
        sidePanel.add(cmykButton);

        // Control panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        controlPanel.setBackground(Color.BLACK);
        controlPanel.setBorder(BorderFactory.createLineBorder(Color.WHITE));
        controlPanel.add(sliderPanel);
        controlPanel.add(togglePanel);

        frame.add(controlPanel, BorderLayout.SOUTH);
        frame.add(sidePanel,    BorderLayout.EAST);
    }

    private void configureToggleButtons(JButton[] buttons,
                                        BooleanSupplier[] getters,
                                        List<Consumer<Boolean>> setters) {
        for (int i = 0; i < buttons.length; i++) {
            JButton btn = buttons[i];
            BooleanSupplier get = getters[i];
            Consumer<Boolean> set = setters.get(i);
            styleButton(btn);
            updateToggleState(btn, get.getAsBoolean());
            
            btn.addActionListener(e -> {
                if (loading) return;
                
                boolean newState = !get.getAsBoolean();
                
                set.accept(newState);
                updateToggleState(btn, newState);
            });
        }
    }

    private void styleButton(JButton btn) {
        btn.setBackground(Color.BLACK);
        btn.setForeground(Color.WHITE);
        btn.setBorder(BorderFactory.createLineBorder(Color.WHITE));
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(130, 40));
    }

    private void updateToggleState(JButton btn, boolean active) {
        btn.setBackground(active ? Color.WHITE : Color.BLACK);
        btn.setForeground(active ? Color.BLACK : Color.WHITE);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void finalizeFrame() {
        frame.pack();
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation((screen.width - frame.getWidth())/2,
                          (screen.height - frame.getHeight())/2);
        frame.setVisible(true);
    }
}