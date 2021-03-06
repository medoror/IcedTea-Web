// Copyright (C) 2001 Jon A. Maxwell (JAM)
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
package net.sourceforge.jnlp.services;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.StringTokenizer;
import javax.jnlp.BasicService;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.adoptopenjdk.icedteaweb.IcedTeaWebConstants;
import net.adoptopenjdk.icedteaweb.StreamUtils;
import net.adoptopenjdk.icedteaweb.client.parts.browser.LinkingBrowser;
import net.adoptopenjdk.icedteaweb.config.validators.ValidatorUtils;
import net.adoptopenjdk.icedteaweb.jnlp.element.information.InformationDesc;
import net.adoptopenjdk.icedteaweb.jnlp.element.resource.JARDesc;
import net.sourceforge.jnlp.JNLPFile;
import net.sourceforge.jnlp.config.ConfigurationConstants;
import net.sourceforge.jnlp.config.DeploymentConfiguration;
import net.sourceforge.jnlp.runtime.ApplicationInstance;
import net.sourceforge.jnlp.runtime.JNLPRuntime;
import net.sourceforge.jnlp.util.logging.OutputController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.adoptopenjdk.icedteaweb.i18n.Translator.R;

/**
 * The BasicService JNLP service.
 *
 * @author <a href="mailto:jmaxwell@users.sourceforge.net">Jon A. Maxwell
 * (JAM)</a> - initial author
 * @version $Revision: 1.10 $
 */
class XBasicService implements BasicService {

    private final static Logger LOG = LoggerFactory.getLogger(XBasicService.class);

    protected XBasicService() {
    }

    /**
     * Returns the codebase of the application, applet, or installer. If the
     * codebase was not specified in the JNLP element then the main JAR's
     * location is returned. If no main JAR was specified then the location of
     * the JAR containing the main class is returned.
     */
    @Override
    public URL getCodeBase() {
        ApplicationInstance app = JNLPRuntime.getApplication();

        if (app != null) {
            JNLPFile file = app.getJNLPFile();

            // return the codebase.
            if (file.getCodeBase() != null) {
                return file.getCodeBase();
            }

            // else return the main JAR's URL.
            JARDesc mainJar = file.getResources().getMainJAR();
            if (mainJar != null) {
                return mainJar.getLocation();
            }

            // else find JAR where main class was defined.
            //
            // JNLPFile file = app.getJNLPFile();
            // String mainClass = file.getEntryPointDesc().getMainClass()+".class";
            // URL jarUrl = app.getClassLoader().getResource(mainClass);
            // go through list of JARDesc to find one matching jarUrl
        }

        return null;
    }

    /**
     * Return true if the Environment is Offline
     */
    @Override
    public boolean isOffline() {

        URL url = findFirstURLFromJNLPFile();
        JNLPRuntime.detectOnline(url);
        return !JNLPRuntime.isOnline();
    }

    /**
     * Return the first URL from the jnlp file Or a default URL if no url found
     * in JNLP file
     */
    private URL findFirstURLFromJNLPFile() {

        ApplicationInstance app = JNLPRuntime.getApplication();

        if (app != null) {
            JNLPFile jnlpFile = app.getJNLPFile();

            URL sourceURL = jnlpFile.getSourceLocation();
            if (sourceURL != null) {
                return sourceURL;
            }

            URL codeBaseURL = jnlpFile.getCodeBase();
            if (codeBaseURL != null) {
                return codeBaseURL;
            }

            InformationDesc informationDesc = jnlpFile.getInformation();
            URL homePage = informationDesc.getHomepage();
            if (homePage != null) {
                return homePage;
            }

            JARDesc[] jarDescs = jnlpFile.getResources().getJARs();
            for (JARDesc jarDesc : jarDescs) {
                return jarDesc.getLocation();
            }
        }

        // this section is only reached if the jnlp file has no jars.
        // that doesn't seem very likely.
        URL arbitraryURL;
        try {
            arbitraryURL = new URL("http://icedtea.classpath.org");
        } catch (MalformedURLException malformedURL) {
            throw new RuntimeException(malformedURL);
        }

        return arbitraryURL;
    }

    /**
     * Return true if a Web Browser is Supported
     */
    @Override
    public boolean isWebBrowserSupported() {
        //there is hardly anything our impl can not handle
        return true;
    }

    /**
     * Show a document.
     *
     * @return whether the document was opened
     */
    @Override
    public boolean showDocument(URL url) {
        try {
//        if (url.toString().endsWith(".jnlp")) {
//            try {
//                new Launcher(false).launchExternal(url);
//                return true;
//            } catch (Exception ex) {
//                return false;
//            }
//        }
// Ignorance of this code is the only regression against original code (if you assume most of the jnlps have jnlp suffix...) we had
// anyway, also jnlp protocol should be handled via this, so while this can be set via 
// ALWAYS-ASK, or directly via BROWSER of deployment.browser.path , it still should be better then it was
// in all cases, the mime recognition is much harder then .jnlp suffix

            String urls = url.toExternalForm();
            LOG.debug("showDocument for: {}", urls);

            DeploymentConfiguration config = JNLPRuntime.getConfiguration();
            String command = config.getProperty(ConfigurationConstants.KEY_BROWSER_PATH);
            //for various debugging
            //command=DeploymentConfiguration.ALWAYS_ASK;
            if (command != null) {
                LOG.debug("{} located. Using: {}", ConfigurationConstants.KEY_BROWSER_PATH, command);
                return exec(command, urls);
            }
            if (System.getenv(ConfigurationConstants.BROWSER_ENV_VAR) != null) {
                command = System.getenv(ConfigurationConstants.BROWSER_ENV_VAR);
                LOG.debug("variable {} located. Using: {}", ConfigurationConstants.BROWSER_ENV_VAR, command);
                return exec(command, urls);
            }
            if (JNLPRuntime.isHeadless() || !Desktop.isDesktopSupported()) {
                command = promptForCommand(urls, false);
                return exec(command, urls);
            } else {
                if (Desktop.isDesktopSupported()) {
                    LOG.debug("using default browser");
                    Desktop.getDesktop().browse(url.toURI());
                    return true;
                } else {
                    LOG.debug("dont know what to do");
                    return false;
                }
            }
        } catch (Exception e) {
            LOG.error(IcedTeaWebConstants.DEFAULT_ERROR_MESSAGE, e);
            return false;
        }
    }

    //cmd form user can contains spaces, quotes and so... now we are relying on default dummy impl
    private boolean exec(String cmd, String url) {
        try {
            if (cmd == null || cmd.length() == 0) {
                return false;
            }
            if (url == null || url.length() == 0) {
                return false;
            }
            if (cmd.equals(ConfigurationConstants.ALWAYS_ASK)) {
                cmd = promptForCommand(url, true);
            }
            if (cmd.equals(ConfigurationConstants.INTERNAL_HTML)) {
                LinkingBrowser.createFrame(url, false, JFrame.DISPOSE_ON_CLOSE);
                return true;
            }
            //copypasted from exec
            StringTokenizer st = new StringTokenizer(cmd + " " + url);
            String[] cmdarray = new String[st.countTokens()];
            for (int i = 0; st.hasMoreTokens(); i++) {
                cmdarray[i] = st.nextToken();
            }
            final ProcessBuilder pb = new ProcessBuilder(cmdarray);
            pb.inheritIO();
            final Process p = pb.start();
            StreamUtils.waitForSafely(p);
            return (p.exitValue() == 0);
        } catch (Exception e) {
            LOG.error(IcedTeaWebConstants.DEFAULT_ERROR_MESSAGE, e);
            try {
                //time for stderr to deal with it in verbose mode
                Thread.sleep(50);
            } catch (Exception ex) {
                //ss
            }
            LOG.info(e.toString());
            LOG.info(DeploymentConfiguration.VVPossibleBrowserValues());
            return false;
        }
    }

    private String promptForCommand(final String targetUrl, boolean aa) throws IOException {
        final String message = DeploymentConfiguration.VVPossibleBrowserValues();
        String title = R("RBrowserLocationPromptTitle");
        if (JNLPRuntime.isHeadless()) {
            OutputController.getLogger().printOutLn(message);
            OutputController.getLogger().printOutLn("*** " + targetUrl + " ***");
            OutputController.getLogger().printOutLn(title);
            String entered = OutputController.getLogger().readLine();
            String verification = ValidatorUtils.verifyFileOrCommand(entered);
            if (verification == null) {
                OutputController.getLogger().printOutLn(R("VVBrowserVerificationFail"));
            } else {
                OutputController.getLogger().printOutLn(R("VVBrowserVerificationPass", verification));
            }
            return entered;
        } else {
            final PromptUrl pu = new PromptUrl();
            pu.arrange(targetUrl, aa);
            pu.setVisible(true);
            return pu.getValue();
        }
    }

    private static class PromptUrl extends JDialog {

        JTextField value = new JTextField("firefox");
        JLabel verification = new JLabel("?");
        private WindowListener cl = new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                value.setText("");
            }
        };
        JCheckBox save = new JCheckBox(R("PESaveChanges"));
        private boolean ask;

        public PromptUrl() {
            super((JDialog) null, R("RBrowserLocationPromptTitle"), true);
        }

        public void arrange(String url, boolean ask) {
            this.ask = ask;
            JPanel top = new JPanel(new GridLayout(2, 1));
            JPanel bottom = new JPanel(new GridLayout(5, 1));
            this.setLayout(new BorderLayout());
            this.add(top, BorderLayout.NORTH);
            this.add(bottom, BorderLayout.SOUTH);
            top.add(new JLabel("<html><b>" + R("RBrowserLocationPromptTitle")));
            JTextField urlField = new JTextField(url);
            urlField.setEditable(false);
            top.add(urlField);
            final JTextArea ta = new JTextArea(DeploymentConfiguration.VVPossibleBrowserValues());
            ta.setEditable(false);
            ta.setLineWrap(true);
            ta.setWrapStyleWord(false);
            JScrollPane scrollableTa=new JScrollPane(ta);
            scrollableTa.setHorizontalScrollBar(null);
            this.add(scrollableTa);
            bottom.add(value);
            bottom.add(verification);
            JButton ok = new JButton(R("ButOk"));
            ok.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (save.isSelected()) {
                        JNLPRuntime.getConfiguration().setProperty(ConfigurationConstants.KEY_BROWSER_PATH, value.getText());
                        try {
                            JNLPRuntime.getConfiguration().save();
                        } catch (IOException ex) {
                            LOG.error(IcedTeaWebConstants.DEFAULT_ERROR_MESSAGE, ex);
                        }
                    }
                    PromptUrl.this.dispose();
                }
            });
            JButton cancel = new JButton(R("ButCancel"));
            cancel.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    cl.windowClosing(null);
                    PromptUrl.this.dispose();
                }
            });
            bottom.add(save);
            bottom.add(ok);
            bottom.add(cancel);
            if (this.ask) {
                save.setSelected(false);
                save.setEnabled(false);
                save.setToolTipText(R("VVBrowserSaveNotAllowed", ConfigurationConstants.ALWAYS_ASK, ConfigurationConstants.KEY_BROWSER_PATH));
            } else {
                save.setEnabled(true);
                save.setToolTipText(R("VVBrowserSaveAllowed", ConfigurationConstants.KEY_BROWSER_PATH));
            }
            this.addWindowListener(cl);

            value.getDocument().addDocumentListener(new DocumentListener() {

                @Override
                public void insertUpdate(DocumentEvent e) {
                    check();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    check();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    check();
                }

                private void check() {
                    String result = ValidatorUtils.verifyFileOrCommand(value.getText());
                    if (result == null) {
                        verification.setForeground(Color.red);
                        verification.setText(R("VVBrowserVerificationFail"));
                        if (!PromptUrl.this.ask) {
                            save.setSelected(false);
                        }
                    } else {
                        verification.setForeground(Color.green);
                        verification.setText(R("VVBrowserVerificationPass", result));
                        if (!PromptUrl.this.ask) {
                            save.setSelected(true);
                        }
                    }
                }
            });
            this.pack();
            this.setSize(500, 400);
        }

        private String getValue() {
            if (value.getText().trim().isEmpty()) {
                return null;
            }
            return value.getText();
        }

    }

}
