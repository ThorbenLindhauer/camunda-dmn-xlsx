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

import java.util.Set;

import org.camunda.bpm.dmn.xlsx.elements.HeaderValuesContainer;
import org.camunda.bpm.dmn.xlsx.elements.IndexedCell;
import org.camunda.bpm.dmn.xlsx.elements.IndexedRow;

/**
 * @author Thorben Lindhauer
 *
 */
public class StaticInputOutputDetectionStrategy implements InputOutputDetectionStrategy {

  protected Set<String> inputColumns;
  protected Set<String> outputColumns;

  public StaticInputOutputDetectionStrategy(Set<String> inputColumns, Set<String> outputColumns) {
    this.inputColumns = inputColumns;
    this.outputColumns = outputColumns;
  }

  public InputOutputColumns determineHeaderCells(IndexedRow headerRow, XlsxWorksheetContext context) {
    InputOutputColumns columns = new InputOutputColumns();

    HeaderValuesContainer hvc;
    for (IndexedCell cell : headerRow.getCells()) {
      if (inputColumns.contains(cell.getColumn())) {
        hvc = new HeaderValuesContainer();
        fillHvc(cell, context, hvc);
        hvc.setId("Input" + cell.getColumn());
        columns.addInputHeaderCell(hvc);
      }
      else if (outputColumns.contains(cell.getColumn())) {
        hvc = new HeaderValuesContainer();
        fillHvc(cell, context, hvc);
        hvc.setId("Output" + cell.getColumn());
        columns.addOutputHeaderCell(hvc);
      }
    }

    return columns;
  }

  @Override
  public String determineHitPolicy(XlsxWorksheetContext context) {
    return null;
  }

  private void fillHvc(IndexedCell cell, XlsxWorksheetContext context, HeaderValuesContainer hvc) {
    hvc.setText(context.resolveCellValue(cell.getCell()));
    hvc.setColumn(cell.getColumn());
  }

  public int numberHeaderRows() {
    return 1;
  }
}
