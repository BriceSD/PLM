package plm.core.ui;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import plm.core.lang.ProgrammingLanguage;
import plm.core.model.Game;
import plm.core.model.session.SourceFile;
import plm.universe.Entity;
import plm.universe.IEntityStackListener;

public class JavaEditorPanel extends RTextScrollPane implements IEditorPanel,IEntityStackListener {
	private static final long serialVersionUID = 1L;
	SourceFile srcFile;
	SourceFileDocumentSynchronizer sync ;
	RSyntaxTextArea codeEditor;
    Entity tracedEntity;

	public JavaEditorPanel(SourceFile srcFile, ProgrammingLanguage lang) {
		super();
		this.srcFile = srcFile;

		codeEditor = new RSyntaxTextArea();
        setViewportView(codeEditor);

        setLineNumbersEnabled(true);
        setFoldIndicatorEnabled(true);

        if (lang.equals(Game.JAVA)) {
            codeEditor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        } else if (lang.equals(Game.PYTHON)) {
            codeEditor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
        } else if (lang.equals(Game.SCALA)) {
            codeEditor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SCALA);
        } else if (lang.equals(Game.C)) {
            codeEditor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_C);
        } else {
            System.err.println("WARNING: Unsupported programming language for syntax highlighting module. Please fix that PLM bug.");
        }

        codeEditor.setAnimateBracketMatching(true);
        codeEditor.setAntiAliasingEnabled(true);
        codeEditor.setAutoIndentEnabled(true);
        codeEditor.setBracketMatchingEnabled(true);
        codeEditor.setCloseCurlyBraces(true);
        codeEditor.setCodeFoldingEnabled(true);

		codeEditor.setCaretPosition(0);
		
		/* Create a synchronization element, and connect it to the editor */
		sync = new SourceFileDocumentSynchronizer(codeEditor);
		sync.setDocument(codeEditor.getDocument());
		codeEditor.getDocument().addDocumentListener(sync);
		
		/* Connect the synchronization element to the source file */
		srcFile.setListener(sync);
		sync.setSourceFile(srcFile);
		
		codeEditor.setText(srcFile.getBody());
        codeEditor.discardAllEdits();
		
		/* Highlighting stuff, to trace entities */
		tracedEntity = Game.getInstance().getSelectedEntity();
		if (tracedEntity != null)
			tracedEntity.addStackListener(this);
	}
	@Override
	public void clear() {
		sync.clear();
		srcFile = null;
		sync=null;
		codeEditor=null;
		if (tracedEntity != null)
			tracedEntity.removeStackListener(this);
	}
	@Override
	public void entityTraceChanged(Entity e, StackTraceElement[] trace) {
		/* TODO:  the following code was not working anymore... 
		for (StackTraceElement elm:trace) {
			// added defenses against NPE because sometimes (launched from a .jar file, a NullPointerException might happen...)
			if (elm.getFileName() != null && codeEditor != null && (elm.getFileName().equals(srcFile.getName()) || elm.getFileName().equals(srcFile.getName()+".java from JavaFileObjectImpl"))) {				
				SyntaxDocument sd = ((SyntaxDocument) codeEditor.getDocument());
				if (sd != null)
					sd.setCurrentEditedLineNumber(elm.getLineNumber());
				codeEditor.repaint();
			}
		}		
		 */
	}
	@Override
	public void tracedEntityChanged(Entity e) {
		if (tracedEntity != null)
			tracedEntity.removeStackListener(this);

		tracedEntity = e;
		if (tracedEntity != null)
			tracedEntity.addStackListener(this);
	}
}
