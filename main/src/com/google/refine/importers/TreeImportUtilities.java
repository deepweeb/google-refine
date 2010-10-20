/*

Copyright 2010, Google Inc.
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

package com.google.refine.importers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;

public abstract class TreeImportUtilities {
    final static Logger logger = LoggerFactory.getLogger("TreeImportUtilities");

    /**
     * An element which holds sub-elements we
     * shall import as records
     */
    static protected class RecordElementCandidate {
        String[] path;
        int count;
    }

    /**
     *
     *
     *
     */
    static protected abstract class ImportVertical {
        public String name = "";
        public int nonBlankCount;

        abstract void tabulate();
    }

    /**
     * A column group describes a branch in tree structured data
     */
    static public class ImportColumnGroup extends ImportVertical {
        public Map<String, ImportColumnGroup> subgroups = new HashMap<String, ImportColumnGroup>();
        public Map<String, ImportColumn> columns = new HashMap<String, ImportColumn>();
        public int nextRowIndex;

        @Override
        void tabulate() {
            for (ImportColumn c : columns.values()) {
                c.tabulate();
                nonBlankCount = Math.max(nonBlankCount, c.nonBlankCount);
            }
            for (ImportColumnGroup g : subgroups.values()) {
                g.tabulate();
                nonBlankCount = Math.max(nonBlankCount, g.nonBlankCount);
            }
        }
    }

    /**
     * A column is used to describe a branch-terminating element in a tree structure
     *
     */
    static public class ImportColumn extends ImportVertical {
        public int      cellIndex;
        public int      nextRowIndex;
        public boolean  blankOnFirstRow;

        public ImportColumn() {}

        public ImportColumn(String name) { //required for testing
            super.name = name;
        }

        @Override
        void tabulate() {
            // already done the tabulation elsewhere
        }
    }

    /**
     * A record describes a data element in a tree-structure
     *
     */
    static public class ImportRecord {
        public List<List<Cell>> rows = new LinkedList<List<Cell>>();
    }

    static protected void sortRecordElementCandidates(List<RecordElementCandidate> list) {
        Collections.sort(list, new Comparator<RecordElementCandidate>() {
            public int compare(RecordElementCandidate o1, RecordElementCandidate o2) {
                return o2.count - o1.count;
            }
        });
    }

    static public void createColumnsFromImport(
            Project project,
            ImportColumnGroup columnGroup
        ) {
            int startColumnIndex = project.columnModel.columns.size();

            List<ImportColumn> columns = new ArrayList<ImportColumn>(columnGroup.columns.values());
            Collections.sort(columns, new Comparator<ImportColumn>() {
                public int compare(ImportColumn o1, ImportColumn o2) {
                    if (o1.blankOnFirstRow != o2.blankOnFirstRow) {
                        return o1.blankOnFirstRow ? 1 : -1;
                    }

                    int c = o2.nonBlankCount - o1.nonBlankCount;
                    return c != 0 ? c : (o1.name.length() - o2.name.length());
                }
            });

            for (int i = 0; i < columns.size(); i++) {
                ImportColumn c = columns.get(i);

                Column column = new com.google.refine.model.Column(c.cellIndex, c.name);
                project.columnModel.columns.add(column);
            }

            List<ImportColumnGroup> subgroups = new ArrayList<ImportColumnGroup>(columnGroup.subgroups.values());
            Collections.sort(subgroups, new Comparator<ImportColumnGroup>() {
                public int compare(ImportColumnGroup o1, ImportColumnGroup o2) {
                    int c = o2.nonBlankCount - o1.nonBlankCount;
                    return c != 0 ? c : (o1.name.length() - o2.name.length());
                }
            });

            for (ImportColumnGroup g : subgroups) {
                createColumnsFromImport(project, g);
            }

            int endColumnIndex = project.columnModel.columns.size();
            int span = endColumnIndex - startColumnIndex;
            if (span > 1 && span < project.columnModel.columns.size()) {
                project.columnModel.addColumnGroup(startColumnIndex, span, startColumnIndex);
            }
        }

    static protected void addCell(
            Project project,
            ImportColumnGroup columnGroup,
            ImportRecord record,
            String columnLocalName,
            String text
        ) {
            if (text == null || ((String) text).isEmpty()) {
                return;
            }

            Serializable value = ImporterUtilities.parseCellValue(text);

            ImportColumn column = getColumn(project, columnGroup, columnLocalName);
            int cellIndex = column.cellIndex;

            int rowIndex = Math.max(columnGroup.nextRowIndex, column.nextRowIndex);
            while (rowIndex >= record.rows.size()) {
                record.rows.add(new ArrayList<Cell>());
            }

            List<Cell> row = record.rows.get(rowIndex);
            while (cellIndex >= row.size()) {
                row.add(null);
            }

            row.set(cellIndex, new Cell(value, null));

            column.nextRowIndex = rowIndex + 1;
            column.nonBlankCount++;
        }


    static protected ImportColumn getColumn(
            Project project,
            ImportColumnGroup columnGroup,
            String localName
        ) {
            if (columnGroup.columns.containsKey(localName)) {
                return columnGroup.columns.get(localName);
            }

            ImportColumn column = createColumn(project, columnGroup, localName);
            columnGroup.columns.put(localName, column);

            return column;
        }

        static protected ImportColumn createColumn(
            Project project,
            ImportColumnGroup columnGroup,
            String localName
        ) {
            ImportColumn newColumn = new ImportColumn();

            newColumn.name =
                columnGroup.name.length() == 0 ?
                (localName == null ? "Text" : localName) :
                (localName == null ? columnGroup.name : (columnGroup.name + " - " + localName));

            newColumn.cellIndex = project.columnModel.allocateNewCellIndex();
            newColumn.nextRowIndex = columnGroup.nextRowIndex;

            return newColumn;
        }

        static protected ImportColumnGroup getColumnGroup(
            Project project,
            ImportColumnGroup columnGroup,
            String localName
        ) {
            if (columnGroup.subgroups.containsKey(localName)) {
                return columnGroup.subgroups.get(localName);
            }

            ImportColumnGroup subgroup = createColumnGroup(project, columnGroup, localName);
            columnGroup.subgroups.put(localName, subgroup);

            return subgroup;
        }

        static protected ImportColumnGroup createColumnGroup(
            Project project,
            ImportColumnGroup columnGroup,
            String localName
        ) {
            ImportColumnGroup newGroup = new ImportColumnGroup();

            newGroup.name =
                columnGroup.name.length() == 0 ?
                (localName == null ? "Text" : localName) :
                (localName == null ? columnGroup.name : (columnGroup.name + " - " + localName));

            newGroup.nextRowIndex = columnGroup.nextRowIndex;

            return newGroup;
        }
}
