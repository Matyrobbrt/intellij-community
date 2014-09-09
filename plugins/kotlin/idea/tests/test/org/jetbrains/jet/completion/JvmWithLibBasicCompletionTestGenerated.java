/*
 * Copyright 2010-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.completion;

import com.intellij.testFramework.TestDataPath;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.junit.runner.RunWith;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.test.InnerTestClasses;
import org.jetbrains.jet.test.TestMetadata;
import org.jetbrains.jet.JUnit3RunnerWithInners;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.jet.generators.tests.TestsPackage}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("idea/testData/completion/basic/custom")
@TestDataPath("$PROJECT_ROOT")
@RunWith(org.jetbrains.jet.JUnit3RunnerWithInners.class)
public class JvmWithLibBasicCompletionTestGenerated extends AbstractJvmWithLibBasicCompletionTest {
    public void testAllFilesPresentInCustom() throws Exception {
        JetTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("idea/testData/completion/basic/custom"), Pattern.compile("^(.+)\\.kt$"), false);
    }
    
    @TestMetadata("NamedArgumentsJava.kt")
    public void testNamedArgumentsJava() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/completion/basic/custom/NamedArgumentsJava.kt");
        doTest(fileName);
    }
    
    @TestMetadata("NamedArgumentsKotlin.kt")
    public void testNamedArgumentsKotlin() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/completion/basic/custom/NamedArgumentsKotlin.kt");
        doTest(fileName);
    }
    
    @TestMetadata("TopLevelNonImportedExtFun.kt")
    public void testTopLevelNonImportedExtFun() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/completion/basic/custom/TopLevelNonImportedExtFun.kt");
        doTest(fileName);
    }
    
    @TestMetadata("TopLevelNonImportedExtProp.kt")
    public void testTopLevelNonImportedExtProp() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/completion/basic/custom/TopLevelNonImportedExtProp.kt");
        doTest(fileName);
    }
    
    @TestMetadata("TopLevelNonImportedFun.kt")
    public void testTopLevelNonImportedFun() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/completion/basic/custom/TopLevelNonImportedFun.kt");
        doTest(fileName);
    }
    
    @TestMetadata("TopLevelNonImportedProperty.kt")
    public void testTopLevelNonImportedProperty() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/completion/basic/custom/TopLevelNonImportedProperty.kt");
        doTest(fileName);
    }
    
}
