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

import java.util.List;

import org.camunda.bpm.dmn.xlsx.elements.HeaderValuesContainer;
import org.camunda.bpm.dmn.xlsx.elements.IndexedCell;
import org.camunda.bpm.dmn.xlsx.elements.IndexedDmnColumns;
import org.camunda.bpm.dmn.xlsx.elements.IndexedRow;
import org.camunda.bpm.model.dmn.Dmn;
import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.camunda.bpm.model.dmn.HitPolicy;
import org.camunda.bpm.model.dmn.impl.DmnModelConstants;
import org.camunda.bpm.model.dmn.instance.*;

/**
 * @author Thorben Lindhauer
 *
 */
public class XlsxWorksheetConverter {

  protected XlsxWorksheetContext worksheetContext;
  protected DmnConversionContext dmnConversionContext;
  protected InputOutputDetectionStrategy ioDetectionStrategy;

  public XlsxWorksheetConverter(XlsxWorksheetContext worksheetContext, InputOutputDetectionStrategy ioDetectionStrategy) {
    this.worksheetContext = worksheetContext;
    this.dmnConversionContext = new DmnConversionContext(worksheetContext);
    this.ioDetectionStrategy = ioDetectionStrategy;

    // order is important; add most specific converters first
    this.dmnConversionContext.addCellContentHandler(new DmnValueRangeConverter());
    this.dmnConversionContext.addCellContentHandler(new FeelSimpleUnaryTestConverter());
    this.dmnConversionContext.addCellContentHandler(new DmnValueStringConverter());
    this.dmnConversionContext.addCellContentHandler(new DmnValueNumberConverter());
  }

  public DmnModelInstance convert() {

    DmnModelInstance dmnModel = initializeEmptyDmnModel();

    Decision decision = generateNamedElement(dmnModel, Decision.class, worksheetContext.getWorksheetName());
    dmnModel.getDefinitions().addChildElement(decision);

    DecisionTable decisionTable = generateElement(dmnModel, DecisionTable.class, "decisionTable");
    decision.addChildElement(decisionTable);

    List<IndexedRow> rows = worksheetContext.getRows();

    setHitPolicy(decisionTable);
    convertInputsOutputs(dmnModel, decisionTable, rows.get(0));
    convertRules(dmnModel, decisionTable, rows.subList(ioDetectionStrategy.numberHeaderRows(), rows.size()));

    return dmnModel;
  }

  protected void setHitPolicy(DecisionTable decisionTable) {
    String hitPolicyString = ioDetectionStrategy.determineHitPolicy(worksheetContext);
    if (hitPolicyString != null) {
      HitPolicy hitPolicy = HitPolicy.valueOf(hitPolicyString);
      decisionTable.setHitPolicy(hitPolicy);
    }
  }

  protected void convertInputsOutputs(DmnModelInstance dmnModel, DecisionTable decisionTable, IndexedRow header) {

    InputOutputColumns inputOutputColumns = ioDetectionStrategy.determineHeaderCells(header, worksheetContext);

    // inputs
    for (HeaderValuesContainer hvc : inputOutputColumns.getInputHeaderCells()) {
      Input input = generateElement(dmnModel, Input.class, hvc.getId());
      decisionTable.addChildElement(input);

      // mandatory
      InputExpression inputExpression = generateElement(dmnModel, InputExpression.class);
      Text text = generateText(dmnModel, hvc.getText());
      inputExpression.setText(text);
      input.setInputExpression(inputExpression);

      // optionals
      if (hvc.getLabel() != null) {
	    input.setLabel(hvc.getLabel());
      }
	  if (hvc.getTypeRef() != null) {
	    inputExpression.setTypeRef(hvc.getTypeRef());
      }
	  if (hvc.getExpressionLanguage() != null) {
	    inputExpression.setExpressionLanguage(hvc.getExpressionLanguage());
      }

      dmnConversionContext.getIndexedDmnColumns().addInput(getIndexedCellForColumn(header, hvc.getColumn()), input);
    }

    // outputs
    for (HeaderValuesContainer hvc : inputOutputColumns.getOutputHeaderCells()) {
      Output output = generateElement(dmnModel, Output.class, hvc.getId());
      decisionTable.addChildElement(output);

      // mandatory
      output.setName(hvc.getText());

      // optionals
      if (hvc.getLabel() != null) {
	      output.setLabel(hvc.getLabel());
      }
      if (hvc.getTypeRef() != null) {
	      output.setTypeRef(hvc.getTypeRef());
      }

      dmnConversionContext.getIndexedDmnColumns().addOutput(getIndexedCellForColumn(header, hvc.getColumn()), output);
    }

  }

  protected IndexedCell getIndexedCellForColumn(IndexedRow header, String column) {
      return header.getCell(column);
  }

  protected void convertRules(DmnModelInstance dmnModel, DecisionTable decisionTable, List<IndexedRow> rulesRows) {
    for (IndexedRow rule : rulesRows) {
      convertRule(dmnModel, decisionTable, rule);
    }
  }

  protected void convertRule(DmnModelInstance dmnModel, DecisionTable decisionTable, IndexedRow ruleRow) {
    Rule rule = generateElement(dmnModel, Rule.class, "excelRow" + ruleRow.getRow().getR());
    decisionTable.addChildElement(rule);

    IndexedDmnColumns dmnColumns = dmnConversionContext.getIndexedDmnColumns();

    for (Input input : dmnColumns.getOrderedInputs()) {
      String xlsxColumn = dmnColumns.getXlsxColumn(input);
      IndexedCell cell = ruleRow.getCell(xlsxColumn);
      String coordinate = xlsxColumn + ruleRow.getRow().getR();

      InputEntry inputEntry = generateElement(dmnModel, InputEntry.class, coordinate);
      String textValue = cell != null ? dmnConversionContext.resolveCellValue(cell.getCell()) : getDefaultCellContent();
      Text text = generateText(dmnModel, textValue);
      inputEntry.setText(text);
      rule.addChildElement(inputEntry);
    }

    for (Output output : dmnColumns.getOrderedOutputs()) {
      String xlsxColumn = dmnColumns.getXlsxColumn(output);
      IndexedCell cell = ruleRow.getCell(xlsxColumn);
      String coordinate = xlsxColumn + ruleRow.getRow().getR();

      OutputEntry outputEntry = generateElement(dmnModel, OutputEntry.class, coordinate);
      String textValue = cell != null ? dmnConversionContext.resolveCellValue(cell.getCell()) : getDefaultCellContent();
      Text text = generateText(dmnModel, textValue);
      outputEntry.setText(text);
      rule.addChildElement(outputEntry);
    }

    IndexedCell annotationCell = ruleRow.getCells().get(ruleRow.getCells().size() - 1);
    Description description =  generateDescription(dmnModel, worksheetContext.resolveCellValue(annotationCell.getCell()));
    rule.setDescription(description);

  }

  protected String getDefaultCellContent() {
    return "-";
  }

  protected DmnModelInstance initializeEmptyDmnModel() {
    DmnModelInstance dmnModel = Dmn.createEmptyModel();
    Definitions definitions = generateNamedElement(dmnModel, Definitions.class, "definitions");
    definitions.setNamespace(DmnModelConstants.CAMUNDA_NS);
    dmnModel.setDefinitions(definitions);

    return dmnModel;
  }

  public <E extends NamedElement> E generateNamedElement(DmnModelInstance modelInstance, Class<E> elementClass, String name) {
    E element = generateElement(modelInstance, elementClass, name);
    element.setName(name);
    return element;
  }

  public <E extends DmnElement> E generateElement(DmnModelInstance modelInstance, Class<E> elementClass, String id) {
    E element = modelInstance.newInstance(elementClass);
    element.setId(id);
    return element;
  }

  /**
   * With a generated id
   */
  public <E extends DmnElement> E generateElement(DmnModelInstance modelInstance, Class<E> elementClass) {
    // TODO: use a proper generator for random IDs
    String generatedId = elementClass.getSimpleName() + Integer.toString((int) (Integer.MAX_VALUE * Math.random()));
    return generateElement(modelInstance, elementClass, generatedId);
  }

  protected Text generateText(DmnModelInstance dmnModel, String content) {
    Text text = dmnModel.newInstance(Text.class);
    text.setTextContent(content);
    return text;
  }

  protected  Description generateDescription(DmnModelInstance dmnModel, String content) {
      Description description =  dmnModel.newInstance(Description.class);
      description.setTextContent(content);
      return description;
  }
}
