/*

Copyright 2011, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package com.google.refine.importers.tree;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.List;

import org.apache.commons.lang.NotImplementedException;
import org.json.JSONObject;

import com.google.refine.ProjectMetadata;
import com.google.refine.importers.ImporterUtilities;
import com.google.refine.importers.ImporterUtilities.MultiFileReadingProgress;
import com.google.refine.importing.ImportingJob;
import com.google.refine.importing.ImportingParser;
import com.google.refine.importing.ImportingUtilities;
import com.google.refine.model.Project;
import com.google.refine.util.JSONUtilities;

abstract public class TreeImportingParserBase implements ImportingParser {
    final protected boolean useInputStream;
    
    protected TreeImportingParserBase(boolean useInputStream) {
        this.useInputStream = useInputStream;
    }
    
    @Override
    public JSONObject createParserUIInitializationData(ImportingJob job,
            List<JSONObject> fileRecords, String format) {
        JSONObject options = new JSONObject();
        return options;
    }
    
    @Override
    public void parse(Project project, ProjectMetadata metadata,
            ImportingJob job, List<JSONObject> fileRecords, String format,
            int limit, JSONObject options, List<Exception> exceptions) {
        
        MultiFileReadingProgress progress = ImporterUtilities.createMultiFileReadingProgress(job, fileRecords);
        ImportColumnGroup rootColumnGroup = new ImportColumnGroup();
        
        for (JSONObject fileRecord : fileRecords) {
            try {
                parseOneFile(project, metadata, job, fileRecord, rootColumnGroup, limit, options, exceptions, progress);
            } catch (IOException e) {
                exceptions.add(e);
            }
            
            if (limit > 0 && project.rows.size() >= limit) {
                break;
            }
        }
        
        XmlImportUtilities.createColumnsFromImport(project, rootColumnGroup);
        project.columnModel.update();
    }
    
    public void parseOneFile(
        Project project,
        ProjectMetadata metadata,
        ImportingJob job,
        JSONObject fileRecord,
        ImportColumnGroup rootColumnGroup,
        int limit,
        JSONObject options,
        List<Exception> exceptions,
        final MultiFileReadingProgress progress
    ) throws IOException {
        final File file = ImportingUtilities.getFile(job, fileRecord);
        final String fileSource = ImportingUtilities.getFileSource(fileRecord);
        
        progress.startFile(fileSource);
        try {
            InputStream inputStream = ImporterUtilities.openAndTrackFile(fileSource, file, progress);
            try {
                if (useInputStream) {
                    parseOneFile(project, metadata, job, fileSource, inputStream,
                            rootColumnGroup, limit, options, exceptions);
                } else {
                    Reader reader = ImportingUtilities.getFileReader(file, fileRecord);
                    parseOneFile(project, metadata, job, fileSource, reader,
                            rootColumnGroup, limit, options, exceptions);
                }
            } finally {
                inputStream.close();
            }
        } finally {
            progress.endFile(fileSource, file.length());
        }
    }
    
    public void parseOneFile(
        Project project,
        ProjectMetadata metadata,
        ImportingJob job,
        String fileSource,
        Reader reader,
        ImportColumnGroup rootColumnGroup,
        int limit,
        JSONObject options,
        List<Exception> exceptions
    ) {
        throw new NotImplementedException();
    }
    
    public void parseOneFile(
        Project project,
        ProjectMetadata metadata,
        ImportingJob job,
        String fileSource,
        InputStream inputStream,
        ImportColumnGroup rootColumnGroup,
        int limit,
        JSONObject options,
        List<Exception> exceptions
    ) {
        throw new NotImplementedException();
    }
    
    protected void parseOneFile(
        Project project,
        ProjectMetadata metadata,
        ImportingJob job,
        String fileSource,
        TreeReader treeParser,
        ImportColumnGroup rootColumnGroup,
        int limit,
        JSONObject options,
        List<Exception> exceptions
    ) {
        String[] recordPath = JSONUtilities.getStringArray(options, "recordPath");
        
        XmlImportUtilities.importTreeData(treeParser, project, recordPath, rootColumnGroup, limit);
    }
}
