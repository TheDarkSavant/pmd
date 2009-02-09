/*
 * Created on 7 mai 2005
 *
 * Copyright (c) 2006, PMD for Eclipse Development Team
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * The end-user documentation included with the redistribution, if
 *       any, must include the following acknowledgement:
 *       "This product includes software developed in part by support from
 *        the Defense Advanced Research Project Agency (DARPA)"
 *     * Neither the name of "PMD for Eclipse Development Team" nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.pmd.eclipse.ui.model;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import net.sourceforge.pmd.eclipse.runtime.PMDRuntimeConstants;
import net.sourceforge.pmd.eclipse.ui.PMDUiConstants;
import net.sourceforge.pmd.eclipse.plugin.PMDPlugin;
import net.sourceforge.pmd.eclipse.ui.nls.StringKeys;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

/**
 * AbstractPMDRecord for Files
 *
 * @author SebastianRaffel ( 16.05.2005 ), Philippe Herlin, Sven Jacob
 *
 */
public class FileRecord extends AbstractPMDRecord {
    
    private AbstractPMDRecord[] children;
    private IResource resource;
    private PackageRecord parent;
    private int numberOfLOC;
    private int numberOfMethods;
    
    public static final IMarker[] emptyMarkers = new IMarker[0];

    /**
     * Constructor (not for use with the Model, no PackageRecord is provided here)
     *
     * @param javaResource the given File
     */
    public FileRecord(IResource javaResource) {
        this(javaResource, null);
    }

    /**
     * Constructor (for use with the Model)
     *
     * @param javaResource
     * @param record
     */
    public FileRecord(IResource javaResource, PackageRecord record) {
        super();

        if (javaResource == null) {
            throw new IllegalArgumentException("javaResouce cannot be null");
        }

        this.resource = javaResource;
        this.parent = record;
        this.numberOfLOC = 0;
        this.numberOfMethods = 0;
        this.children = createChildren();
    }

    /**
     *  @see net.sourceforge.pmd.eclipse.ui.model.AbstractPMDRecord#getParent()
     */
    @Override
    public AbstractPMDRecord getParent() {
        return this.parent;
    }

    /**
     * @see net.sourceforge.pmd.eclipse.ui.model.AbstractPMDRecord#getChildren()
     */
    @Override
    public AbstractPMDRecord[] getChildren() {
        return children;  // NOPMD by Sven on 13.11.06 11:57
    }

    /**
     * @see net.sourceforge.pmd.eclipse.ui.model.AbstractPMDRecord#getResource()
     */
    @Override
    public IResource getResource() {
        return this.resource;
    }

    /**
     * Updates all children.
     *
     */
    public void updateChildren() {
        this.children = createChildren();
    }

    /**
     * @see net.sourceforge.pmd.eclipse.ui.model.AbstractPMDRecord#createChildren()
     */
    @Override
    protected final AbstractPMDRecord[] createChildren() {
        AbstractPMDRecord[] children = AbstractPMDRecord.EMPTY_RECORDS;

        try {
            // get all markers
            final List<IMarker> markers = Arrays.asList(this.findMarkers());
            final Iterator<IMarker> markerIterator = markers.iterator();

            // put all markers in a map with key = rulename
            final Map<String, MarkerRecord> allMarkerMap = new HashMap<String, MarkerRecord>();
            while (markerIterator.hasNext()) {
                final IMarker marker = markerIterator.next();

                MarkerRecord markerRecord = allMarkerMap.get(marker.getAttribute(PMDUiConstants.KEY_MARKERATT_RULENAME));
                if (markerRecord == null) {
                    String ruleName = (String)marker.getAttribute(PMDUiConstants.KEY_MARKERATT_RULENAME);
                    markerRecord = new MarkerRecord(this,  // NOPMD by Sven on 13.11.06 11:57
                            ruleName,
                            ((Integer)marker.getAttribute(PMDUiConstants.KEY_MARKERATT_PRIORITY)).intValue());
                    markerRecord.addViolation(marker);
                    allMarkerMap.put(ruleName, markerRecord);
                } else {
                    markerRecord.addViolation(marker);
                }
            }

            children = allMarkerMap.values().toArray(new MarkerRecord[allMarkerMap.size()]);
        } catch (CoreException e) {
            PMDPlugin.getDefault().logError(StringKeys.MSGKEY_ERROR_CORE_EXCEPTION + this.toString(), e);
        }

        return children; // no children so return an empty array, not null!
    }

    /**
     * Checks the File for PMD-Markers
     *
     * @return true if the File has PMD-Markers, false otherwise
     */
    @Override
    public boolean hasMarkers() {
        final IMarker[] markers = findMarkers();
        return markers != null && markers.length > 0;
    }

    /**
     * Finds PMD-Markers in the File
     *
     * @return an Array of markers
     */
    @Override
    public final IMarker[] findMarkers() {
        
        try {
            // this is the overwritten Function from AbstractPMDRecord
            // we simply call the IResource-function to find Markers
            if (this.resource.isAccessible()) {
                return this.resource.findMarkers(PMDRuntimeConstants.PMD_MARKER, true, IResource.DEPTH_INFINITE);
            }
        } catch (CoreException ce) {
            PMDPlugin.getDefault().logError(StringKeys.MSGKEY_ERROR_FIND_MARKER + this.toString(), ce);
        }

        return emptyMarkers;
    }

    /**
     * Finds PMD PDFA Markers in the File
     *
     * @return an Array of markers
     */
    public IMarker[] findDFAMarkers() {
        
        try {
            // we can only find Markers for a file
            // we use the DFA-Marker-ID set for Dataflow Anomalies
            if (this.resource.isAccessible()) {
                return this.resource.findMarkers(PMDRuntimeConstants.PMD_DFA_MARKER, true, IResource.DEPTH_INFINITE);
            }
        } catch (CoreException ce) {
            PMDPlugin.getDefault().logError(StringKeys.MSGKEY_ERROR_FIND_MARKER + this.toString(), ce);
        }

        return emptyMarkers;
    }

    /**
     * Finds Markers, that have a given Attribute with a given Value
     *
     * @param attributeName
     * @param value
     * @return an Array of markers matching these Attribute and Value
     */
    @Override
    public IMarker[] findMarkersByAttribute(String attributeName, Object value) {
        final IMarker[] markers = findMarkers();
        final List<IMarker> attributeMarkers = new ArrayList<IMarker>();
        try {
            // we get all Markers and catch the ones that matches our criteria
            for (IMarker marker : markers) {
                final Object val = marker.getAttribute(attributeName);

                // if the value is null, the Attribute doesn't exist
                if (val != null && val.equals(value)) {
                    attributeMarkers.add(marker);
                }
            }
        } catch (CoreException ce) {
            PMDPlugin.getDefault().logError(StringKeys.MSGKEY_ERROR_FIND_MARKER + this.toString(), ce);
        }

        // return an Array of the Markers
        return attributeMarkers.toArray(new IMarker[attributeMarkers.size()]);
    }

    /**
     * Calculates the Number of Code-Lines this File has.
     * The Function is adapted from the Eclipse Metrics-Plugin available at:
     * http://www.sourceforge.net/projects/metrics
     *
     * @return the Lines of Code
     */
    public void calculateLinesOfCode() {
        if (this.resource.isAccessible()) {

            // the whole while has to be a String for this operation
            // so we read the File line-wise into a String
            int loc = 0;
            final String source = resourceToString(this.resource);
            final int firstCurly = source.indexOf('{');
            if (firstCurly != -1) {
                final String body = source.substring(firstCurly+1, source.length()-1).trim();
                final StringTokenizer lines = new StringTokenizer(body, "\n");
                while(lines.hasMoreTokens()) {
                    String trimmed = lines.nextToken().trim();
                    if (trimmed.length() > 0 && trimmed.startsWith("/*")) {
                        while (trimmed.indexOf("*/") == -1) {
                            trimmed = lines.nextToken().trim();
                        }
                        if (lines.hasMoreTokens()) { // NOPMD by Sven on 13.11.06 12:04
                            trimmed = lines.nextToken().trim();
                        }
                    }

                    if (!trimmed.startsWith("//")) {
                        loc++;
                    }
                }
            }

            this.numberOfLOC = loc;
        }
    }

    /**
     * Gets the Number of Code-Lines this File has.
     *
     * @return the Lines of Code
     */
    @Override
    public int getLOC() {
        return this.numberOfLOC;
    }

    /**
     * Reads a Resource's File and return the Code as String.
     *
     * @param resource a resource to read ; the resource must be accessible.
     * @return a String which is the Files Content
     */
    protected String resourceToString(IResource resource) {
        final StringBuffer fileContents = new StringBuffer();
        BufferedReader bReader = null;
        try {
            // we create a FileReader
            bReader = new BufferedReader(new FileReader(resource.getRawLocation().toFile()));

            // ... and read the File line by line
            while (bReader.ready()) {
                fileContents.append(bReader.readLine()).append('\n');
            }
        } catch (FileNotFoundException fnfe) {
            PMDPlugin.getDefault().logError(
                    StringKeys.MSGKEY_ERROR_FILE_NOT_FOUND + resource.toString() + " in " + this.toString(), fnfe);
        } catch (IOException ioe) {
            PMDPlugin.getDefault().logError(StringKeys.MSGKEY_ERROR_IO_EXCEPTION + this.toString(), ioe);
        } finally {
            if (bReader != null) {
                try {
                    bReader.close();
                } catch (IOException e) { // NOPMD by Herlin on 07/10/06 17:47
                    // ignore
                }
            }
        }

        return fileContents.toString();
    }

    /**
     * Calculate the number of methods.
     */
    public void calculateNumberOfMethods() {
        if (this.resource.isAccessible()) {

            // we need to change the Resource into a Java-File
            final IJavaElement element = JavaCore.create(this.resource);
            final List<Object> methods = new ArrayList<Object>();

            if (element instanceof ICompilationUnit) {
                try {
                    // ITypes can be Package Declarations or other Java Stuff too
                    final IType[] types = ((ICompilationUnit) element).getTypes();
                    for (IType type : types) {
                        // only if it is an IType itself, it's a Class
                        // from which we can get its Methods
                        methods.addAll( Arrays.asList(type.getMethods() ));
                    }
                } catch (JavaModelException jme) {
                    PMDPlugin.getDefault().logError(
                            StringKeys.MSGKEY_ERROR_JAVAMODEL_EXCEPTION + this.toString(), jme);
                }
            }
            if (!methods.isEmpty()) {
                this.numberOfMethods = methods.size();
            }
        }
    }

    /**
     * Gets the Number of Methods, this class contains.
     *
     * @return the Number of Methods
     */
    @Override
    public int getNumberOfMethods() {
        return this.numberOfMethods; // deactivate this method for now
    }

    /**
     *  @see net.sourceforge.pmd.eclipse.ui.model.AbstractPMDRecord#addResource(org.eclipse.core.resources.IResource)
     */
    @Override
    public AbstractPMDRecord addResource(IResource resource) {
        return null;
    }

    /**
     *  @see net.sourceforge.pmd.eclipse.ui.model.AbstractPMDRecord#removeResource(org.eclipse.core.resources.IResource)
     */
    @Override
    public AbstractPMDRecord removeResource(IResource resource) {
        return null;
    }

    /**
     * @see net.sourceforge.pmd.eclipse.ui.model.AbstractPMDRecord#getName()
     */
    @Override
    public String getName() {
        return this.resource.getName();
    }

    /**
     *  @see net.sourceforge.pmd.eclipse.ui.model.AbstractPMDRecord#getResourceType()
     */
    @Override
    public int getResourceType() {
        return AbstractPMDRecord.TYPE_FILE;
    }

    /**
     * @see net.sourceforge.pmd.eclipse.ui.model.AbstractPMDRecord#getNumberOfViolationsToPriority(int)
     */
    @Override
    public int getNumberOfViolationsToPriority(int prio, boolean invertMarkerAndFileRecords) {
        int number = 0;

        for (AbstractPMDRecord element : children) {
            number += element.getNumberOfViolationsToPriority(prio, false);
        }
        return number;
    }
}