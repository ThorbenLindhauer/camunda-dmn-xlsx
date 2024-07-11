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
package org.camunda.bpm.xlsx.plugin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.camunda.bpm.dmn.xlsx.AdvancedSpreadsheetAdapter;
import org.camunda.bpm.dmn.xlsx.XlsxConverter;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.persistence.deploy.Deployer;
import org.camunda.bpm.engine.impl.persistence.entity.DeploymentEntity;
import org.camunda.bpm.engine.impl.persistence.entity.ResourceEntity;
import org.camunda.bpm.model.dmn.Dmn;
import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.yaml.snakeyaml.Yaml;

/**
 * Converts an xlsx file to a dmn file on the fly and deploys the dmn table
 *
 * @author Thorben Lindhauer
 */
public class XlsxDeployer implements Deployer {

  public static final String XLSX_RESOURCE_SUFFIX = "xlsx";

  public void deploy(DeploymentEntity deployment) {
    if (!deployment.isNew()) {
      // only generate dmn xml once when deploying for the first time
      return;
    }

    List<ResourceEntity> generatedResources = new ArrayList<ResourceEntity>();

    for (ResourceEntity resource : deployment.getResources().values()) {
      if (canHandle(resource)) {
        XlsxDeploymentMetaData metaData = loadMetaData(deployment, resource.getName() + ".yaml");
        DmnModelInstance dmnModel = generateDmnModel(resource, metaData);
        ResourceEntity dmnResource = generateDeploymentResource(deployment, dmnModel, generateDmnResourceName(resource));
        generatedResources.add(dmnResource);
      }
    }

    addToDeployment(generatedResources, deployment);
  }

  protected ResourceEntity generateDeploymentResource(DeploymentEntity deployment, DmnModelInstance dmnModel, String name) {
    ResourceEntity resource = new ResourceEntity();

    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    Dmn.writeModelToStream(byteStream, dmnModel);
    byte[] bytes = byteStream.toByteArray();

    resource.setName(name);
    resource.setBytes(bytes);
    resource.setDeploymentId(deployment.getId());

    // Mark the resource as 'generated'
    resource.setGenerated(true);

    return resource;
  }

  protected void addToDeployment(List<ResourceEntity> generatedResources, DeploymentEntity deployment) {
    for (ResourceEntity resource : generatedResources) {
      Context.getCommandContext().getResourceManager().insert(resource);

      deployment.addResource(resource);
    }

  }

  protected String generateDmnResourceName(ResourceEntity xlsxResource) {
    return xlsxResource.getName() + ".dmn";
  }

  protected boolean canHandle(ResourceEntity resource) {
    return resource.getName().endsWith("." + XLSX_RESOURCE_SUFFIX);
  }

  protected DmnModelInstance generateDmnModel(ResourceEntity xlsxResource, XlsxDeploymentMetaData metaData) {
    byte[] bytes = xlsxResource.getBytes();
    ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);

    XlsxConverter converter = new XlsxConverter();
    if (metaData != null) {
      metaData.applyTo(converter);
    } else {
      converter.setIoDetectionStrategy(new AdvancedSpreadsheetAdapter());
    }
    return converter.convert(inputStream);
  }

  protected XlsxDeploymentMetaData loadMetaData(DeploymentEntity deployment, String metaDataResource) {

    ResourceEntity resource = deployment.getResource(metaDataResource);

    if (resource != null) {
      ByteArrayInputStream inputStream = new ByteArrayInputStream(resource.getBytes());

      Yaml yaml = new Yaml();
      return yaml.loadAs(inputStream, XlsxDeploymentMetaData.class);
    }
    else {
      return null;
    }
  }

}
