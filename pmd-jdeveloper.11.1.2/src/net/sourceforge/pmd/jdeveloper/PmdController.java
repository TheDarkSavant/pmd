package net.sourceforge.pmd.jdeveloper;

import oracle.ide.Context;
import oracle.ide.Ide;
import oracle.ide.controller.Controller;
import oracle.ide.controller.IdeAction;
import oracle.ide.extension.RegisteredByExtension;


/**
 * The controller implementation is responsible for enabling and disabling
 * actions and performing any work required when they are invoked.
 */
 @RegisteredByExtension("net.sourceforge.pmd.jdeveloper")
public final class PmdController implements Controller {
    /**
     * Look up the numeric id of the action we defined in our extension
     * manifest. The string constant matches the value of the id attribute in
     * the action element of the extension manifest.<p>
     *
     * The numeric id can be used to retrieve an instance of IdeAction.
     */
    public static final int RUN_PMD_CMD_ID =
        Ide.findCmdID("net.sourceforge.pmd.jdeveloper.CheckPmd");

    public boolean handleEvent(final IdeAction action, final Context context) {
        // Command is handled by net.sourceforge.pmd.jdeveloper.PmdCommand
        return false;
    }

    public boolean update(final IdeAction action, final Context context) {
       action.setEnabled(true);
       return true;
    }
}
