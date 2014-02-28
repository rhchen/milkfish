/**********************************************************************
 * Copyright (c) 2013 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Matthew Khouzam- Initial API and implementation
 **********************************************************************/

package net.sf.milkfish.product.cli;

import java.util.HashMap;
import java.util.Map;

import net.sf.milkfish.product.messages.Messages;

/**
 * Command line parser
 *
 * @author Matthew Khouzam
 */
public class CliParser {

    private static final String NOUI_ARG = "--NOUI"; //$NON-NLS-1$

    private static final String OPEN_ARG = "--open"; //$NON-NLS-1$

    private final Map<String, String> params = new HashMap<String, String>();

    /** Open key     */
    public static final String OPEN_FILE_LOCATION = ".,-=open=-,."; //$NON-NLS-1$
    /** No ui key    */
    public static final String NO_UI = ".,-=noui=-,."; //$NON-NLS-1$

    /**
     * Constructor
     *
     * @param args
     *            the command line arguments
     * @throws CliException
     *             an error occurred parsing the cli
     */
    public CliParser(final String[] args) throws CliException {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals(OPEN_ARG)) {
                put(OPEN_FILE_LOCATION, args, i);
                // skip since we have two args
                i++;
            }
            else if (args[i].equals(NOUI_ARG)) {
                params.put(NO_UI, new String());
            }
        }
    }

    private void put(String key, String[] args, int pos) throws CliException {
        if (args.length <= pos) {
            throw new CliException(Messages.CliParser_MalformedCommand + ':' + ' ' + args[pos]);
        }
        params.put(key, args[pos + 1]);
    }

    /**
     * Get a parameter from the parsed command line
     * @param key OPEN_FILE_LOCATION or NO_UI
     * @return the value of the parameter, can be null
     */
    public String getArgument(String key) {
        return params.get(key);
    }

}
