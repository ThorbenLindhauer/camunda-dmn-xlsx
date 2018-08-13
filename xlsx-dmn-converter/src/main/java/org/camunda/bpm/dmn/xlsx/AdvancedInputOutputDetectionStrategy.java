/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.bpm.dmn.xlsx;

import org.camunda.bpm.dmn.xlsx.elements.HeaderValuesContainer;
import org.camunda.bpm.dmn.xlsx.elements.IndexedCell;
import org.camunda.bpm.dmn.xlsx.elements.IndexedRow;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AdvancedInputOutputDetectionStrategy implements InputOutputDetectionStrategy {

  protected Set<String> inputColumns;
  protected Set<String> outputColumns;

  public AdvancedInputOutputDetectionStrategy() {
    this.inputColumns = new LinkedHashSet<String>();
    this.outputColumns = new LinkedHashSet<String>();
  }

  public InputOutputColumns determineHeaderCells(IndexedRow headerRow, XlsxWorksheetContext context) {
      List<IndexedCell> cells = headerRow.getCells();
      for (IndexedCell indexedCell: cells) {
          if("input".equalsIgnoreCase(context.resolveCellValue(indexedCell.getCell()))) {
              inputColumns.add(indexedCell.getColumn());
          }
          if("output".equalsIgnoreCase(context.resolveCellValue(indexedCell.getCell()))) {
              outputColumns.add(indexedCell.getColumn());
          }
      }

    InputOutputColumns columns = new InputOutputColumns();
    int idCounter = 0;
    for (String column: inputColumns) {
      idCounter++;
      HeaderValuesContainer hvc = new HeaderValuesContainer();
      hvc.setId("input" + idCounter);
      fillHvc(context, column, hvc);
      columns.addInputHeaderCell(hvc);
    }
    idCounter= 0;
    for (String column: outputColumns) {
      idCounter++;
      HeaderValuesContainer hvc = new HeaderValuesContainer();
      hvc.setId("output" + idCounter);
      fillHvc(context, column, hvc);
      columns.addOutputHeaderCell(hvc);
    }

    return columns;
  }

  public String determineHitPolicy(XlsxWorksheetContext context) {
    if ( context.getRows().size() < 4) {
      return null;
    }
    IndexedRow row = context.getRows().get(4);
    if (row.getCell("A")!=null) {
      return context.resolveCellValue(row.getCell("A").getCell()).toUpperCase();
    }

    return null;
  }

  private void fillHvc(XlsxWorksheetContext context, String column, HeaderValuesContainer hvc) {
    IndexedCell cell;
    cell = context.getRows().get(1).getCell(column);
    hvc.setLabel(context.resolveCellValue(cell.getCell()));
    cell = context.getRows().get(2).getCell(column);
    hvc.setExpressionLanguage(context.resolveCellValue(cell.getCell()));
    cell = context.getRows().get(3).getCell(column);
    hvc.setText(context.resolveCellValue(cell.getCell()));
    cell = context.getRows().get(4).getCell(column);
    hvc.setTypeRef(context.resolveCellValue(cell.getCell()));
    hvc.setColumn(column);
  }

  public int numberHeaderRows() {
    return 5;
  }
}
