// Copyright 2020 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.language.pure.compiler.test.fromGrammar;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.Milestoning;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.junit.Assert;
import org.junit.Test;

public class TestDomainCompilationFromGrammar extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{
    @Override
    public String getDuplicatedElementTestCode()
    {
        return "Class anything::class {}\n" +
                "###Mapping\n" +
                "Mapping anything::somethingelse ()\n" +
                "###Pure\n" +
                "Class anything::somethingelse\n" +
                "{\n" +
                "}\n";
    }

    @Override
    public String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [5:1-7:1]: Duplicated element 'anything::somethingelse'";
    }

    @Test
    public void testPartialCompilationDuplicatedElement()
    {
        partialCompilationTest("Class anything::class {}\n" +
                "###Mapping\n" +
                "Mapping anything::somethingelse ()\n" +
                "###Pure\n" +
                "Class anything::somethingelse\n" +
                "{\n" +
                "}\n", Lists.fixedSize.with("COMPILATION error at [5:1-7:1]: Duplicated element 'anything::somethingelse'"));
    }

    @Test
    public void testDuplicatedDomainElements()
    {
        String initialGraph = "Class anything::class\n" +
                "{\n" +
                "  ok : Integer[0..1];\n" +
                "}\n" +
                "###Mapping\n" +
                "Mapping anything::somethingelse\n" +
                "(\n" +
                ")\n";
        // Class
        test(initialGraph +
                "###Pure\n" +
                "Class anything::somethingelse\n" +
                "{\n" +
                "}\n", "COMPILATION error at [10:1-12:1]: Duplicated element 'anything::somethingelse'"
        );
        // Profile
        test(initialGraph +
                "###Pure\n" +
                "Profile anything::somethingelse\n" +
                "{\n" +
                "}\n", "COMPILATION error at [10:1-12:1]: Duplicated element 'anything::somethingelse'"
        );
        // Enumeration
        test(initialGraph +
                "###Pure\n" +
                "Enum anything::somethingelse\n" +
                "{\n" +
                " A\n" +
                "}\n", "COMPILATION error at [10:1-13:1]: Duplicated element 'anything::somethingelse'"
        );
        // Association
        test(initialGraph +
                "###Pure\n" +
                "Class test::B\n" +
                "{\n" +
                "  good : Integer[0..1];\n" +
                "}\n" +
                "Association anything::somethingelse\n" +
                "{\n" +
                "  b1 : anything::class[1];\n" +
                "  b2 : test::B[1];\n" +
                "}\n", "COMPILATION error at [14:1-18:1]: Duplicated element 'anything::somethingelse'"
        );
        // Function
        test(initialGraph +
                "###Pure\n" +
                "function anything::somethingelse(a:String[1]):String[1]\n" +
                "{\n" +
                "   'hiiii'\n" +
                "}\n");
        // Measure
        test(initialGraph +
                "###Pure\n" +
                "Measure anything::somethingelse\n" +
                "{\n" +
                "   *UnitOne: x -> $x;\n" +
                "   UnitTwo: x -> $x * 1000;\n" +
                "   UnitThree: x -> $x * 400;\n" +
                "}", "COMPILATION error at [10:1-15:1]: Duplicated element 'anything::somethingelse'"
        );
    }

    @Test
    public void testMetaFunctionExecutionWithFullPath()
    {
        String code =
                "function example::testMaxInteger(input: Integer[1]):Any[0..1]\n" +
                        "{\n" +
                        "   [1,$input]->meta::pure::functions::math::max();\n" +
                        "}\n";
        test(code);
    }

    @Test
    public void testMetaFunctionExecutionWithoutFullPath()
    {
        String code =
                "function example::testMaxInteger(input: Integer[1]):Any[0..1]\n" +
                        "{\n" +
                        "   [1,$input]->max();\n" +
                        "}\n";
        test(code);
    }

    @Test
    public void testUserDefinedFunctionWithTheSameNameButDifferentSignatureExecutionWithImports()
    {
        String code =
                "function example::testMaxInteger(input: Integer[1]):Any[0..1]\n" +
                        "{\n" +
                        "   [1,$input]->max();\n" +
                        "}\n" +
                        "function example::testMaxInteger():Any[0..1]\n" +
                        "{\n" +
                        "   [1,2]->max();\n" +
                        "}\n" + "###Pure\n" +
                        "import example::*;\n" +
                        "function example::test::go():Any[0..1]\n" +
                        "{\n" +
                        "   testMaxInteger(1);\n" +
                        "   testMaxInteger();\n" +
                        "}\n";
        test(code);
    }

    @Test
    public void testUserDefinedFunctionWithTheSameNameButDifferentSignatureExecution()
    {
        String code =
                "function example::testMaxInteger(input: Integer[1]):Any[0..1]\n" +
                        "{\n" +
                        "   [1,$input]->max();\n" +
                        "}\n" +
                        "function example::testMaxInteger(input: Number[1]):Any[0..1]\n" +
                        "{\n" +
                        "   [1,2]->max();\n" +
                        "}\n" +
                        "function example::testMaxInteger(f: Float[1], d: Float[1]):Any[0..1]\n" +
                        "{\n" +
                        "   [1, $f, $d]->max();\n" +
                        "}\n" +
                        "function example::testMaxInteger():Any[0..1]\n" +
                        "{\n" +
                        "   [1,2]->max();\n" +
                        "}\n" +
                        "function example::test::testMaxInteger():Any[0..1]\n" +
                        "{\n" +
                        "   [1,2]->max();\n" +
                        "}\n" +
                        "function example::test::go():Any[0..1]\n" +
                        "{\n" +
                        "   example::testMaxInteger(1);\n" +
                        "   example::testMaxInteger();\n" +
                        "   example::test::testMaxInteger();\n" +
                        "   example::testMaxInteger(1.0, 1.123);\n" +
                        "}\n";
        test(code);
    }

    @Test
    public void testUserDefinedFunctionWithTheSameSignature()
    {
        String code =
                "function example::testMaxInteger(input: Integer[1]):Any[0..1]\n" +
                        "{\n" +
                        "   [1,$input]->max();\n" +
                        "}\n" +
                        "function example::testMaxInteger(input: Integer[1]):Any[0..1]\n" +
                        "{\n" +
                        "   [1,2]->max();\n" +
                        "}\n";
        test(code, "COMPILATION error at [5:1-8:1]: Duplicated element 'example::testMaxInteger_Integer_1__Any_$0_1$_'");
    }

    @Test
    public void testCycleClassSuperType()
    {
        test("Class test::A extends test::A\n" +
                "{\n" +
                "   ok : Integer[0..1];\n" +
                "}\n", "COMPILATION error at [1:1-4:1]: Cycle detected in class supertype hierarchy: test::A -> test::A"
        );
        test("Class test::A extends test::B\n" +
                "{\n" +
                "   ok : Integer[0..1];\n" +
                "}\n" +
                "Class test::B extends test::A\n" +
                "{\n" +
                "   ok : Integer[0..1];\n" +
                "}\n", "COMPILATION error at [1:1-4:1]: Cycle detected in class supertype hierarchy: test::A -> test::B -> test::A"
        );
        test("Class test::A extends test::B\n" +
                "{\n" +
                "   ok : Integer[0..1];\n" +
                "}\n" +
                "Class test::B extends test::C\n" +
                "{\n" +
                "   ok : Integer[0..1];\n" +
                "}\n" +
                "Class test::C extends test::A\n" +
                "{\n" +
                "   ok : Integer[0..1];\n" +
                "}\n", "COMPILATION error at [1:1-4:1]: Cycle detected in class supertype hierarchy: test::A -> test::B -> test::C -> test::A"
        );
    }

    @Test
    public void testDuplicateProfileTagAndStereotypeWarning()
    {
        test("Profile test::A\n" +
                "{\n" +
                "   tags : [doc, doc];\n" +
                "   stereotypes : [modifier, modifier, accessorType, accessorType];\n" +
                "}\n", null, Lists.fixedSize.with("COMPILATION error at [1:1-5:1]: Found duplicated stereotype 'accessorType' in profile 'test::A'", "COMPILATION error at [1:1-5:1]: Found duplicated stereotype 'modifier' in profile 'test::A'", "COMPILATION error at [1:1-5:1]: Found duplicated tag 'doc' in profile 'test::A'"));
    }

    @Test
    public void testDuplicateEnumValueWarning()
    {
        test("Enum test::A\n" +
                "{\n" +
                "   TEA,COFFEE,TEA,TEA,COFFEE\n" +
                "}\n", null, Lists.fixedSize.with("COMPILATION error at [3:4-6]: Found duplicated value 'TEA' in enumeration 'test::A'", "COMPILATION error at [3:8-13]: Found duplicated value 'COFFEE' in enumeration 'test::A'"));
    }

    @Test
    public void testDuplicateAssociationPropertyWarning()
    {
        test("Class test::A {}\n" +
                "Class test::B {}\n" +
                "Association test::C\n" +
                "{\n" +
                "   property1: test::A[0..1];\n" +
                "   property1: test::B[1];\n" +
                "}\n", null, Lists.fixedSize.with("COMPILATION error at [5:4-28]: Found duplicated property 'property1' in association 'test::C'"));
    }

    @Test
    public void testDuplicateClassPropertyWarning()
    {
        test("Class test::A\n" +
                "{\n" +
                "   property : Integer[0..1];\n" +
                "   property : String[1];\n" +
                "   other : String[1];\n" +
                "   ok : String[1];\n" +
                "   other: String[1];\n" +
                "}\n", null, Lists.fixedSize.with("COMPILATION error at [3:4-28]: Found duplicated property 'property' in class 'test::A'", "COMPILATION error at [5:4-21]: Found duplicated property 'other' in class 'test::A'"));
    }

    @Test
    public void testPartialCompilationDuplicateAssociationPropertyWarningAndDuplicateClassPropertyWarning()
    {
        partialCompilationTest("Class test::A {}\n" +
                "Class test::B {}\n" +
                "Association test::C\n" +
                "{\n" +
                "   property1: test::A[0..1];\n" +
                "   property1: test::B[1];\n" +
                "}\n" +
                "Class test::D\n" +
                "{\n" +
                "   property : Integer[0..1];\n" +
                "   property : String[1];\n" +
                "   other : String[1];\n" +
                "   ok : String[1];\n" +
                "   other: String[1];\n" +
                "}\n", null, Lists.fixedSize.with("COMPILATION error at [5:4-28]: Found duplicated property 'property1' in association 'test::C'", "COMPILATION error at [10:4-28]: Found duplicated property 'property' in class 'test::D'", "COMPILATION error at [12:4-21]: Found duplicated property 'other' in class 'test::D'"));
    }

    @Test
    public void testSuperTypeDuplication()
    {
        test("Class test::A\n" +
                "{\n" +
                "   ok : Integer[0..1];\n" +
                "}\n" +
                "Class test::B extends test::A, test::A\n" +
                "{\n" +
                "   ok : Integer[0..1];\n" +
                "}\n", "COMPILATION error at [5:1-8:1]: Duplicated super type 'test::A' in class 'test::B'"
        );
    }

    @Test
    public void testSimpleClass()
    {
        test("Class test::A\n" +
                "{\n" +
                "   ok : Integer[0..1];\n" +
                "   test : Integer[0..1];\n" +
                "}\n" +
                "Class test::B \n" +
                "{\n" +
                " good : Integer[0..1];\n" +
                "}\n"
        );
    }

    @Test
    public void testPackageWithUnderscore()
    {
        test("function my::functionParent():String[1]\n" +
                "{\n" +
                "    my::package_with_underscore::functionName();\n" +
                "}\n" +
                "\n" +
                "function my::package_with_underscore::functionName():String[1]\n" +
                "{\n" +
                " 'result';\n" +
                "}");
    }

    @Test
    public void testElementDefinitionWithoutPackage()
    {
        test("Class A\n" +
                "{\n" +
                "}\n", "COMPILATION error at [1:1-3:1]: Element package is required"
        );
    }

    @Test
    public void testMeasureDefinition()
    {
        test("Measure test::NewMeasure\n" +
                "{\n" +
                "   *UnitOne: x -> $x;\n" +
                "   UnitTwo: x -> $x * 1000;\n" +
                "   UnitThree: x -> $x * 400;\n" +
                "}"
        );
    }

    @Test
    public void testNonConvertibleMeasureDefinition()
    {
        test("Measure test::NewNonConvertibleMeasure\n" +
                "{\n" +
                "   UnitOne;\n" +
                "   UnitTwo;\n" +
                "   UnitThree;\n" +
                "}"
        );
    }

    @Test
    public void testClassWithUnitTypeProperty()
    {
        String newMeasure = "Measure test::NewMeasure\n" +
                "{\n" +
                "   *UnitOne: x -> $x;\n" +
                "   UnitTwo: x -> $x * 1000;\n" +
                "   UnitThree: x -> $x * 400;\n" +
                "}";
        test(newMeasure +
                "Class test::A\n" +
                "{\n" +
                "   unitOne : test::NewMeasure~UnitOne[0..1];\n" +
                "   unitTwo : test::NewMeasure~UnitTwo[0..1];\n" +
                "}\n"
        );
    }

    @Test
    public void testClassWithNonConvertibleUnitTypeProperty()
    {
        String newMeasure = "Measure test::NewNonConvertibleMeasure\n" +
                "{\n" +
                "   UnitOne;\n" +
                "   UnitTwo;\n" +
                "   UnitThree;\n" +
                "}";
        test(newMeasure +
                "Class test::A\n" +
                "{\n" +
                "   unitOne : test::NewNonConvertibleMeasure~UnitOne[0..1];\n" +
                "   unitTwo : test::NewNonConvertibleMeasure~UnitTwo[0..1];\n" +
                "}\n"
        );
    }

    @Test
    public void testClassWithMissingUnitType()
    {
        String newMeasure = "Measure test::NewMeasure\n" +
                "{\n" +
                "   *UnitOne: x -> $x;\n" +
                "   UnitTwo: x -> $x * 1000;\n" +
                "   UnitThree: x -> $x * 400;\n" +
                "}";
        String expectedErrorMessage = "COMPILATION error at [8:15-39]: Can't find type 'test::NewMeasure~UnitFour'";
        test(newMeasure +
                "Class test::A\n" +
                "{\n" +
                "   unitFour : test::NewMeasure~UnitFour[0..1];\n" +
                "}\n", expectedErrorMessage
        );
    }

    @Test
    public void testMissingProfile()
    {
        test("Class <<NoProfile.NoKey>> test::A\n" +
                "{\n" +
                "   ok : Integer[0..1];\n" +
                "}\n", "COMPILATION error at [1:9-17]: Can't find the profile 'NoProfile'");
    }

    @Test
    public void testMissingTaggedValue()
    {
        test("Profile meta::pure::profiles::doc\n" +
                "{\n" +
                "   stereotypes: [tests];\n" +
                "   tags: [doc, todo];\n" +
                "}\n" +
                "Class test::A\n" +
                "{\n" +
                "<<meta::pure::profiles::doc.imMissing>> ok: Integer[0..1];\n" +
                "}\n", "COMPILATION error at [8:3-37]: Can't find stereotype 'imMissing' in profile 'meta::pure::profiles::doc'");
    }

    @Test
    public void testMissingStereoType()
    {
        test("Profile meta::pure::profiles::doc\n" +
                "{\n" +
                "   stereotypes: [tests];\n" +
                "   tags: [doc, todo];\n" +
                "}\n" +
                "Class {meta::pure::profiles::doc.imMissing = 'imMissing'} test::A\n" +
                "{\n" +
                "ok: Integer[0..1];\n" +
                "}\n", "COMPILATION error at [6:34-42]: Can't find tag 'imMissing' in profile 'meta::pure::profiles::doc'");
    }

    @Test
    public void testMissingSuperType()
    {
        test("Class test::A\n" +
                "{\n" +
                "   ok : Integer[0..1];\n" +
                "}\n" +
                "\n" +
                "Class test::B extends NotHere\n" +
                "{\n" +
                "}\n", "COMPILATION error at [6:23-29]: Can't find type 'NotHere'"
        );
    }

    @Test
    public void testFaultyClassSuperType()
    {
        test("Enum test::A\n" +
                "{\n" +
                "   A, B , C\n" +
                "}\n" +
                "\n" +
                "Class test::B extends test::A\n" +
                "{\n" +
                "}\n", "COMPILATION error at [6:1-8:1]: Invalid supertype: 'B' cannot extend 'test::A' as it is not a class."
        );
    }

    @Test
    public void testMissingPropertyType()
    {
        test("Class test::A\n" +
                "{\n" +
                "   good: String[0..1];\n" +
                "   notGood : MissingProp[1];\n" +
                "}\n", "COMPILATION error at [4:14-24]: Can't find type 'MissingProp'"
        );
    }

    @Test
    public void testFaultyAssociation()
    {
        test("Association test::FaultyAssociation\n" +
                "{\n" +
                "   a : String[1];\n" +
                "}\n" +
                "\n", "COMPILATION error at [1:1-4:1]: Expected 2 properties for an association 'test::FaultyAssociation'"
        );
        test("Association test::FaultyAssociation\n" +
                "{\n" +
                "   a : String[1];\n" +
                "   b : String[1];\n" +
                "   c : String[1];\n" +
                "}\n", "COMPILATION error at [1:1-6:1]: Expected 2 properties for an association 'test::FaultyAssociation'"
        );
    }


    @Test
    public void testPrimitive()
    {
        test("Class test::A\n" +
                "[\n" +
                "  constraint1: $this.ok->toOne() == 1,\n" +
                "  constraint2: if($this.ok == 'ok', |true, |false),\n" +
                "  constraint3: $this.anyValue->instanceOf(String) || $this.anyValue->instanceOf(test::AEnum)\n" +
                "]\n" +
                "{\n" +
                "  name: String[45..*];\n" +
                "  name1: Boolean[45..*];\n" +
                "  ok: Integer[1..2];\n" +
                "  ok1: Number[1..2];\n" +
                "  ok2: Decimal[1..2];\n" +
                "  ok3: Float[1..2];\n" +
                "  ok4: Date[1..2];\n" +
                "  ok5: StrictDate[1..2];\n" +
                "  ok6: DateTime[1..2];\n" +
                "  ok7: LatestDate[1..2];\n" +
                "  ok8: StrictTime[1..2];\n" +
                "  anyValue: meta::pure::metamodel::type::Any[1];\n" +
                "}\n" +
                "\n" +
                "Enum test::AEnum\n" +
                "{\n" +
                "  B\n" +
                "}\n");
    }

    @Test
    public void testComplexConstraint()
    {
        test("Class test::A\n" +
                "[\n" +
                "  constraint1\n" +
                "  (" +
                "    ~externalId: 'ext ID'\n" +
                "    ~function: if($this.ok == 'ok', |true, |false)\n" +
                "    ~enforcementLevel: Warn\n" +
                "    ~message: $this.ok + ' is not ok'\n" +
                "  )\n" +
                "]\n" +
                "{\n" +
                "  ok: Integer[1..2];\n" +
                "}\n");
    }

    @Test
    public void testPartialCompilationComplexConstraint()
    {
        partialCompilationTest("Class test::A\n" +
                "[\n" +
                "  constraint1\n" +
                "  (" +
                "    ~externalId: 'ext ID'\n" +
                "    ~function: if($this.ok == 'ok', |true, |false)\n" +
                "    ~enforcementLevel: Warn\n" +
                "    ~message: $this.ok + ' is not ok'\n" +
                "  )\n" +
                "]\n" +
                "{\n" +
                "  ok: Integer[1..2];\n" +
                "}\n");
    }

    @Test
    public void testFunctionWithExpressionInParameter()
    {
        test("Class test::A\n" +
                "[\n" +
                "  constraint1\n" +
                "  (" +
                "    ~externalId: 'ext ID'\n" +
                "    ~function: greaterThanEqual($this.start, $this.end-1000)\n" +
                "    ~enforcementLevel: Warn\n" +
                "    ~message: $this.start + ' should be greater or equal ' + $this.end\n" +
                "  )\n" +
                "]\n" +
                "{\n" +
                "  start: Integer[1];\n" +
                "  end: Integer[1];\n" +
                "}\n");
    }

    @Test
    public void testFunctionOrLambdaWithUnknownToken()
    {
        test("Class test::A\n" +
                        "{\n" +
                        "   name : String[*];\n" +
                        "   xza(z:String[1]){ok}:String[1];\n" +
                        "}\n",
                "COMPILATION error at [4:21-22]: Can't find the packageable element 'ok'");
        test("Class test::A\n" +
                "[\n" +
                "   ok\n" +
                "]\n" +
                "{\n" +
                "   names : String[*];\n" +
                "}", "COMPILATION error at [3:4-5]: Can't find the packageable element 'ok'");
        test("Class test::b\n" +
                "{\n" +
                "   names : String[*];\n" +
                "}\n" +
                "Class test::A\n" +
                "[\n" +
                "   test::a\n" +
                "]\n" +
                "{\n" +
                "   names : String[*];\n" +
                "}", "COMPILATION error at [7:4-10]: Can't find the packageable element 'test::a'");
        test("Class test::b\n" +
                "{\n" +
                "   names : String[*];\n" +
                "}\n" +
                "Class test::A\n" +
                "[\n" +
                "   test::b\n" +
                "]\n" +
                "{\n" +
                "   names : String[*];\n" +
                "}", "COMPILATION error at [7:4-10]: Constraint must be of type 'Boolean'");
    }

    @Test
    public void testFunctionOrLambdaWithUnknownEnumValue()
    {
        test("Class test::A\n" +
                "{\n" +
                "   name : String[*];\n" +
                "   xza(z:String[1]){ok.a}:String[1];\n" +
                "}\n", "COMPILATION error at [4:21-22]: Can't find the packageable element 'ok'");
        test("Enum test::b\n" +
                "{\n" +
                "   names\n" +
                "}\n" +
                "Class test::A\n" +
                "[\n" +
                "   test::b.c\n" +
                "]\n" +
                "{\n" +
                "   names : String[*];\n" +
                "}", "COMPILATION error at [7:12]: Can't find enum value 'c' in enumeration 'test::b'");
        test("Class test::b\n" +
                "{\n" +
                "   names : String[*];\n" +
                "}\n" +
                "Class test::A\n" +
                "[\n" +
                "   test::b.c\n" +
                "]\n" +
                "{\n" +
                "   names : String[*];\n" +
                "}", "COMPILATION error at [7:12]: Can't find property 'c' in class 'meta::pure::metamodel::type::Class'");
    }

    @Test
    public void testMissingAssociationProperty()
    {
        test("Class test::A\n" +
                "{\n" +
                "}\n" +
                "Association test::FaultyAssociation\n" +
                "{\n" +
                "   a : test::A[1];\n" +
                "   b : someClass[1];\n" +
                "}\n", "COMPILATION error at [7:4-20]: Can't find class 'someClass'");
    }

    @Test
    public void testQualifiedProperty()
    {
        test("Class test::A\n" +
                "{\n" +
                "   name : String[*];\n" +
                "   xza(s:z::k::B[1]){$s + 'ok'}:String[1];\n" +
                "}\n", "COMPILATION error at [4:10-16]: Can't find type 'z::k::B'");
    }

    @Test
    public void testMissingAnyAppliedProperty()
    {
        test("Class test::A\n" +
                "{\n" +
                "   name : String[*];\n" +
                "}\n" +
                "Class test::B\n" +
                "{\n" +
                "   xza(s: test::A[1]){$s.x + 'ok'}:String[1];\n" +
                "}\n", "COMPILATION error at [7:26]: Can't find property 'x' in class 'test::A'");
    }

    @Test
    public void testMissingEnumValueInConstraint()
    {
        test("Enum test::PriceExpressionEnum {\n" +
                "   AbsoluteTerms,\n" +
                "   ParcentageOfNotional\n" +
                "}\n" +
                "\n" +
                "Class ui::String {\n" +
                "  prop1: String[1];\n" +
                "}\n" +
                "\n" +
                "Class ui::b extends ui::String\n" +
                "[\n" +
                "   if($this.priceExpression == test::PriceExpressionEnum.PercetateOfNotionall, |$this.currency->isEmpty(), |true)\n" +
                "]\n" +
                "{\n" +
                "  priceExpression: test::PriceExpressionEnum[1];\n" +
                "  currency: String[*];\n" +
                "}", "COMPILATION error at [12:58-77]: Can't find enum value 'PercetateOfNotionall' in enumeration 'test::PriceExpressionEnum'");
    }

    @Test
    public void testMissingEnumValueInDerivedProperty()
    {
        test("Enum test::PriceExpressionEnum {\n" +
                "   AbsoluteTerms,\n" +
                "   ParcentageOfNotional\n" +
                "}\n" +
                "\n" +
                "Class ui::String {\n" +
                "  prop1: String[1];\n" +
                "}\n" +
                "\n" +
                "Class ui::b extends ui::String\n" +
                "{\n" +
                "  priceExpression: test::PriceExpressionEnum[1];\n" +
                "  currency: String[*];\n" +
                "  clingy() {\n" +
                "    test::PriceExpressionEnum.PercetateOfNotionall;\n" +
                "    'sad';\n" +
                "  }: String[*];\n" +
                "}", "COMPILATION error at [15:31-50]: Can't find enum value 'PercetateOfNotionall' in enumeration 'test::PriceExpressionEnum'");
    }

    @Test
    public void testMissingLoopedAnyAppliedProperty()
    {
        test("Class test::Dog\n" +
                "{\n" +
                "   name : String[*];\n" +
                "}\n" +
                "Class test::B\n" +
                "{\n" +
                "   pet: test::Dog[1];\n" +
                "}\n" +
                "Class test::C\n" +
                "{\n" +
                "   xza(s: test::B[1]){$s.pet.dogMissing + 'ok'}:String[1];\n" +
                "}\n", "COMPILATION error at [11:30-39]: Can't find property 'dogMissing' in class 'test::Dog'");
    }

    @Test
    public void testGoodLoopedQualified()
    {
        test("Class test::Dog\n" +
                "{\n" +
                "   name : String[*];\n" +
                "}\n" +
                "Class test::B\n" +
                "{\n" +
                "   pet: test::Dog[1];\n" +
                "}\n" +
                "Class test::C\n" +
                "{\n" +
                "   xza(s: test::B[1]){$s.pet.name->at(0) + 'ok'}:String[1];\n" +
                "}\n");
    }

    @Test
    public void testGoodQualifiedProperty()
    {
        test("Class test::Dog\n" +
                "{\n" +
                "    funcDog(){'my Name is Bobby';}:String[1];\n" +
                "}\n" +
                "Class test::B\n" +
                "{\n" +
                "    funcB(s: test::Dog[1]){$s.funcDog()}:String[1];\n" +
                "}\n" +
                "Class test::C\n" +
                "{\n" +
                "   test(s: test::B[1], d: test::Dog[1]){$s.funcB($d) + '!'}:String[1];\n" +
                "}\n");
    }

    @Test
    public void testFailedToFindQualifiedProperty()
    {
        test("Class test::Dog\n" +
                "{\n" +
                "    funcDog(){'my Name is Bobby';}:String[1];\n" +
                "}\n" +
                "Class test::B\n" +
                "{\n" +
                "    funcB(s: test::Dog[1]){$s.WhoopsfuncDog()}:String[1];\n" +
                "}\n" +
                "Class test::C\n" +
                "{\n" +
                "   test(s: test::B[1], d: test::Dog[1]){$s.funcB($d) + '!'}:String[1];\n" +
                "}\n", "COMPILATION error at [7:31-43]: Can't find property 'WhoopsfuncDog' in class 'test::Dog'");
    }

    @Test
    public void testMissingVariableName()
    {
        test("Enum test::A\n" +
                "{\n" +
                "   A, a \n" +
                "}\n" +
                "Class test::B\n" +
                "{\n" +
                "   xza(s: String[1]){$src}:String[1];\n" +
                "}\n", "COMPILATION error at [7:22-25]: Can't find variable class for variable 'src' in the graph");
    }

    @Test
    public void testPartialCompilationFailedToFindQualifiedPropertyAndMissingVariableName()
    {
        partialCompilationTest("Class test::Dog\n" +
                "{\n" +
                "    funcDog(){'my Name is Bobby';}:String[1];\n" +
                "}\n" +
                "Class test::B\n" +
                "{\n" +
                "    funcB(s: test::Dog[1]){$s.WhoopsfuncDog()}:String[1];\n" +
                "}\n" +
                "Class test::C\n" +
                "{\n" +
                "   test(s: test::B[1], d: test::Dog[1]){$s.funcB($d) + '!'}:String[1];\n" +
                "}\n" +
                "Enum test::A\n" +
                "{\n" +
                "   A, a \n" +
                "}\n" +
                "Class test::D\n" +
                "{\n" +
                "   xza(s: String[1]){$src}:String[1];\n" +
                "}\n", Lists.fixedSize.with("COMPILATION error at [7:31-43]: Can't find property 'WhoopsfuncDog' in class 'test::Dog'", "COMPILATION error at [19:22-25]: Can't find variable class for variable 'src' in the graph"));
    }

    @Test
    public void testMapLambdaInferenceWithPrimitive()
    {
        test("Class test::A\n" +
                "{\n" +
                "   p(){[1,2]->map(a|$a+1)}:Integer[*];\n" +
                "}"
        );
        test("Class test::A\n" +
                "{\n" +
                "   p(){[1,2]->map(a|$a+'1')}:String[1];\n" +
                "}", "COMPILATION error at [3:23-26]: Can't find a match for function 'plus(Any[2])'");
    }

    @Test
    public void testPairInference()
    {
        test("Class test::A\n" +
                "{\n" +
                "   p(){pair(1, 'a')}:Pair<Integer, String>[1];\n" +
                "}"
        );
    }

    @Test
    public void testMapLambdaInferenceWithClass()
    {
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->map(a|$a.name)}:String[*];\n" +
                "}");
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->map(a|$a.nam)}:String[*];\n" +
                "}", "COMPILATION error at [8:32-34]: Can't find property 'nam' in class 'test::A'");
    }

    @Test
    public void testPackageableElementMismatchNotFoundWithGetAll()
    {
        test("Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->map(a|$a.nam)}:String[*];\n" +
                "}", "COMPILATION error at [3:8-14]: Can't find the packageable element 'test::A'");
    }

    @Test
    public void testPackageableElementMismatchWithGetAll()
    {
        test("###Pure\n" +
                "Class test::A\n" +
                "{\n" +
                "   prop : String[1];\n" +
                "}\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::MyMapping.all()->map(a|$a.nam)}:String[*];\n" +
                "}\n" +
                "###Mapping\n" +
                "Mapping test::MyMapping\n" +
                "(\n" +
                ")\n", "COMPILATION error at [8:23-28]: Can't find a match for function 'getAll(Mapping[1])'");
    }

    @Test
    public void testSortByLambdaInferenceWithClass()
    {
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->sortBy(a|$a.name)}:test::A[*];\n" +
                "}\n");
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->sortBy(a|$a.nam)}:test::A[*];\n" +
                "}", "COMPILATION error at [8:35-37]: Can't find property 'nam' in class 'test::A'");
    }

    @Test
    public void testPartialCompilationPackageableElementMismatchWithGetAllAndSortByLambdaInferenceWithClass()
    {
        partialCompilationTest("###Pure\n" +
                "Class test::A\n" +
                "{\n" +
                "   prop : String[1];\n" +
                "}\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::MyMapping.all()->map(a|$a.nam)}:String[*];\n" +
                "}\n" +
                "Class test::C\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::D\n" +
                "{\n" +
                "   z(){test::C.all()->sortBy(a|$a.nam)}:test::C[*];\n" +
                "}\n" +
                "###Mapping\n" +
                "Mapping test::MyMapping\n" +
                "(\n" +
                ")\n", Lists.fixedSize.with("COMPILATION error at [8:23-28]: Can't find a match for function 'getAll(Mapping[1])'", "COMPILATION error at [17:35-37]: Can't find property 'nam' in class 'test::C'"));
    }

    @Test
    public void testFilterLambdaInferenceWithClass()
    {
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->filter(a|$a.name == 'yeah')}:test::A[*];\n" +
                "}\n");
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->filter(a|$a.nam == 'ohoh')}:test::A[*];\n" +
                "}", "COMPILATION error at [8:35-37]: Can't find property 'nam' in class 'test::A'");
    }

    @Test
    public void testGroupByLambdaInferenceWithClass()
    {
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->groupBy(a|$a.name, agg(x|$x.name, z|$z->count()), ['a', 'b'])}:meta::pure::tds::TabularDataSet[1];\n" +
                "}");
//        test("Class test::A\n" +
//                "{\n" +
//                "   name : String[1];\n" +
//                "}\n" +
//                "\n" +
//                "Class test::B\n" +
//                "{\n" +
//                "   z(){agg(x:test::A[1]|$x.name, z:String[1]|$z->count())}:meta::pure::functions::collection::AggregateValue<test::A, String, Integer>[1];\n" +
//                "}");
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->groupBy(a|$a.nae, agg(x|$x.name, z|$z->count()), ['a', 'b'])}:meta::pure::tds::TabularDataSet[1];\n" +
                "}", "COMPILATION error at [8:36-38]: Can't find property 'nae' in class 'test::A'");
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->groupBy(a|$a.name, agg(x|$x.nae, z|$z->count()), ['a', 'b'])}:meta::pure::tds::TabularDataSet[1];\n" +
                "}", "COMPILATION error at [8:51-53]: Can't find property 'nae' in class 'test::A'");
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->groupBy(a|$a.name, agg(x|$x.name, z|$z->map(k|$k+1)), ['a', 'b'])}:meta::pure::tds::TabularDataSet[1];\n" +
                "}", "COMPILATION error at [8:71-72]: Can't find a match for function 'plus(Any[2])'");
    }

    @Test
    public void testGroupByWithWindowLambdaInferenceWithClass()
    {
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->groupByWithWindowSubset(a|$a.name, agg(x|$x.name, z|$z->count()), ['a', 'b'], ['a'], ['b'])}:meta::pure::tds::TabularDataSet[1];\n" +
                "}\n");
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->groupByWithWindowSubset(a|$a.name, agg(x|$x.namex, z|$z->count()), ['a', 'b'], ['a'], ['b'])}:meta::pure::tds::TabularDataSet[1];\n" +
                "}", "COMPILATION error at [8:67-71]: Can't find property 'namex' in class 'test::A'");
    }

    @Test
    public void testProjectInferenceWithClass()
    {
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->project([a|$a.name], ['a', 'b'])}:meta::pure::tds::TabularDataSet[1];\n" +
                "   g(){test::A.all()->project(a|$a.name, ['a', 'b'])}:meta::pure::tds::TabularDataSet[1];\n" +
                "}\n");
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->project([a|$a.name.name], ['a'])}:meta::pure::tds::TabularDataSet[1];\n" +
                "}", "COMPILATION error at [8:42-45]: The property 'name' can't be accessed on primitive types. Inferred primitive type is String");
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->project([a|$a.nawme], ['a', 'b'])}:meta::pure::tds::TabularDataSet[1];\n" +
                "}", "COMPILATION error at [8:37-41]: Can't find property 'nawme' in class 'test::A'");
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->project(a|$a.nawme, ['a', 'b'])}:meta::pure::tds::TabularDataSet[1];\n" +
                "}", "COMPILATION error at [8:36-40]: Can't find property 'nawme' in class 'test::A'");
    }

    @Test
    public void testProjectColInferenceWithClass()
    {
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->project([col(a|$a.name, 'a')])}:meta::pure::tds::TabularDataSet[1];\n" +
                "   y(){test::A.all()->project(col(a|$a.name, 'a'))}:meta::pure::tds::TabularDataSet[1];\n" +
                "}");
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->project([col(a|$a.naxme, 'a')])}:meta::pure::tds::TabularDataSet[1];\n" +
                "}", "COMPILATION error at [8:41-45]: Can't find property 'naxme' in class 'test::A'");
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->project(col(a|$a.naxme, 'a'))}:meta::pure::tds::TabularDataSet[1];\n" +
                "}", "COMPILATION error at [8:40-44]: Can't find property 'naxme' in class 'test::A'");
    }

    @Test
    public void testProjectWithSubsetColInferenceWithClass()
    {
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->projectWithColumnSubset([col(a|$a.name, 'a')], ['a','b'])}:meta::pure::tds::TabularDataSet[1];\n" +
                "   y(){test::A.all()->projectWithColumnSubset(col(a|$a.name, 'a'), ['a','b'])}:meta::pure::tds::TabularDataSet[1];\n" +
                "   h(){test::A.all()->projectWithColumnSubset([a|$a.name], 'a', ['a','b'])}:meta::pure::tds::TabularDataSet[1];\n" +
                "   j(){test::A.all()->projectWithColumnSubset(a|$a.name, 'a' , ['a','b'])}:meta::pure::tds::TabularDataSet[1];\n" +
                "}");

        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->projectWithColumnSubset([col(a|$a.xname, 'a')], ['a','b'])}:meta::pure::tds::TabularDataSet[1];\n" +
                "}", "COMPILATION error at [8:57-61]: Can't find property 'xname' in class 'test::A'");
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   y(){test::A.all()->projectWithColumnSubset(col(a|$a.xname, 'a'), ['a','b'])}:meta::pure::tds::TabularDataSet[1];\n" +
                "}", "COMPILATION error at [8:56-60]: Can't find property 'xname' in class 'test::A'");
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   h(){test::A.all()->projectWithColumnSubset([a|$a.xname], 'a', ['a','b'])}:meta::pure::tds::TabularDataSet[1];\n" +
                "}", "COMPILATION error at [8:53-57]: Can't find property 'xname' in class 'test::A'");
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   j(){test::A.all()->projectWithColumnSubset(a|$a.xname, 'a' , ['a','b'])}:meta::pure::tds::TabularDataSet[1];\n" +
                "}", "COMPILATION error at [8:52-56]: Can't find property 'xname' in class 'test::A'");
    }

    @Test
    public void testPartialCompilationProjectColInferenceWithClassAndProjectWithSubsetColInferenceWithClass()
    {
        partialCompilationTest("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->project([col(a|$a.name, 'a')])}:meta::pure::tds::TabularDataSet[1];\n" +
                "   y(){test::A.all()->project(col(a|$a.name, 'a'))}:meta::pure::tds::TabularDataSet[1];\n" +
                "}", null);

        partialCompilationTest("Class test::C\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::D\n" +
                "{\n" +
                "   z(){test::C.all()->project([col(c|$c.naxme, 'c')])}:meta::pure::tds::TabularDataSet[1];\n" +
                "}\n" +
                "Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->projectWithColumnSubset([col(a|$a.xname, 'a')], ['a','b'])}:meta::pure::tds::TabularDataSet[1];\n" +
                "}", Lists.fixedSize.with("COMPILATION error at [8:41-45]: Can't find property 'naxme' in class 'test::C'", "COMPILATION error at [17:57-61]: Can't find property 'xname' in class 'test::A'"));

        partialCompilationTest("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->project(col(a|$a.naxme, 'a'))}:meta::pure::tds::TabularDataSet[1];\n" +
                "}", Lists.fixedSize.with("COMPILATION error at [8:40-44]: Can't find property 'naxme' in class 'test::A'"));
    }

    @Test
    public void testExistsLambdaInferenceWithClass()
    {
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->exists(a|$a.name == 'yeah')}:Boolean[1];\n" +
                "}");
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->exists(a|$a.nam == 'ohoh')}:test::A[*];\n" +
                "}", "COMPILATION error at [8:35-37]: Can't find property 'nam' in class 'test::A'");
    }

    @Test
    public void testTDSContainsInferenceWithClass()
    {
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->filter(a|$a->tdsContains([p|$p.name], test::A.all()->project(col(a|$a.name, 'ww'))))}:test::A[*];\n" +
                "   k(){test::A.all()->filter(a|$a->tdsContains(p|$p.name, test::A.all()->project(col(a|$a.name, 'ww'))))}:test::A[*];\n" +
                "}\n");
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->filter(a|$a->tdsContains([p|$p.xname], test::A.all()->project(col(a|$a.name, 'ww'))))}:test::A[*];\n" +
                "}", "COMPILATION error at [8:54-58]: Can't find property 'xname' in class 'test::A'");
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   k(){test::A.all()->filter(a|$a->tdsContains(p|$p.xname, test::A.all()->project(col(a|$a.name, 'ww'))))}:test::A[*];\n" +
                "}", "COMPILATION error at [8:53-57]: Can't find property 'xname' in class 'test::A'");
    }

    @Test
    public void testTDSContainsWithLambdaInferenceWithClass()
    {
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->filter(v|$v->tdsContains([p|$p.name], ['a'], test::A.all()->project(col(a|$a.name, 'ww')), {a,b | $a.isNotNull('name') && $b.isNotNull('Addr_Name')}))}:test::A[*];\n" +
                "   z(){test::A.all()->filter(v|$v->tdsContains(p|$p.name, ['a'], test::A.all()->project(col(a|$a.name, 'ww')), {a,b | $a.isNotNull('name') && $b.isNotNull('Addr_Name')}))}:test::A[*];\n" +
                "}\n");
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->filter(v|$v->tdsContains([p|$p.name], ['a'], test::A.all()->project(col(a|$a.ncame, 'ww')), {a,b | $a.isNotXNull('name') && $b.isNotNull('Addr_Name')}))}:test::A[*];\n" +
                "}", "COMPILATION error at [8:100-104]: Can't find property 'ncame' in class 'test::A'");
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->filter(v|$v->tdsContains([p|$p.name], ['a'], test::A.all()->project(col(a|$a.name, 'ww')), {a,b | $a.isNotXNull('name') && $b.isNotNull('Addr_Name')}))}:test::A[*];\n" +
                "}", "COMPILATION error at [8:124-133]: Can't find property 'isNotXNull' in class 'meta::pure::tds::TDSRow'");
    }

    @Test
    public void testGroupByTDS()
    {
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->project([col(a|$a.name, 'Account_No')])->groupBy('prodName', agg('sum', x|$x.getFloat('quantity')*$x.getInteger('quantity'), y| $y->sum()))}:meta::pure::tds::TabularDataSet[1];\n" +
                "}");
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->project([col(a|$a.name, 'Account_No')])->groupBy('prodName', agg('sum', x|$x.getwFloat('quantity')*$x.getInteger('quantity'), y| $y->sum()))}:meta::pure::tds::TabularDataSet[1];\n" +
                "}", "COMPILATION error at [8:100-108]: Can't find property 'getwFloat' in class 'meta::pure::tds::TDSRow'");
    }

    @Test
    public void testOlapGroupByTDS()
    {
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "   age : Integer[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->project([col(p|$p.name, 'Name'), col(p|$p.age, 'Age')])->olapGroupBy( ['age'],desc('age'), func(y|$y->count()),'testCol')}:meta::pure::tds::TabularDataSet[1];\n" +
                "}");
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "   age : Integer[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->project([col(p|$p.name, 'Name'), col(p|$p.age, 'Age')])->olapGroupBy( ['age'],desc('age'), y|$y->count(),'testCol')}:meta::pure::tds::TabularDataSet[1];\n" +
                "}");
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "   age : Integer[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->project([col(a|$a.name, 'Account_No')])->olapGroupBy( ['age'],desc('age'), func('age', y|$y->count()),'testCol')}:meta::pure::tds::TabularDataSet[1];\n" +
                "}");
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "   age : Integer[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->project([col(p|$p.name, 'Name'), col(p|$p.age, 'Age')])->olapGroupBy( ['age'], func(y|$y->rank()),'testCol')}:meta::pure::tds::TabularDataSet[1];\n" +
                "}");
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "   age : Integer[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->project([col(p|$p.name, 'Name'), col(p|$p.age, 'Age')])->olapGroupBy( ['age'], y|$y->rank(),'testCol')}:meta::pure::tds::TabularDataSet[1];\n" +
                "}");
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "   age : Integer[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->project([col(p|$p.name, 'Name'), col(p|$p.age, 'Age')])->olapGroupBy( ['age'], func('age', y|$y->max()),'testCol')}:meta::pure::tds::TabularDataSet[1];\n" +
                "}");
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "   age : Integer[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->project([col(p|$p.name, 'Name'), col(p|$p.age, 'Age')])->olapGroupBy(desc('age'), func( y|$y->denseRank()),'testCol')}:meta::pure::tds::TabularDataSet[1];\n" +
                "}");
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "   age : Integer[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->project([col(p|$p.name, 'Name'), col(p|$p.age, 'Age')])->olapGroupBy(asc('age'), y|$y->count(),'testCol')}:meta::pure::tds::TabularDataSet[1];\n" +
                "}");
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "   age : Integer[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->project([col(p|$p.name, 'Name'), col(p|$p.age, 'Age')])->olapGroupBy(desc('age'), func('age', y|$y->max()),'testCol')}:meta::pure::tds::TabularDataSet[1];\n" +
                "}");

        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "   age : Integer[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->project([col(p|$p.name, 'Name'), col(p|$p.age, 'Age')])->olapGroupBy(func(y|$y->count()),'testCol')}:meta::pure::tds::TabularDataSet[1];\n" +
                "}");
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "   age : Integer[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->project([col(p|$p.name, 'Name'), col(p|$p.age, 'Age')])->olapGroupBy(y|$y->count(),'testCol')}:meta::pure::tds::TabularDataSet[1];\n" +
                "}");
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "   age : Integer[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){test::A.all()->project([col(p|$p.name, 'Name'), col(p|$p.age, 'Age')])->olapGroupBy(func('age',y|$y->min()),'testCol')}:meta::pure::tds::TabularDataSet[1];\n" +
                "}");
    }

    @Test
    public void testMultiplicityErrorInCollection()
    {
        test("Class test::A\n" +
                "{\n" +
                "   names : String[*];\n" +
                "   prop() {$this.names->at(0) + 'ok'} : String[1];\n" +
                "}");
        test("Class test::A\n" +
                "{\n" +
                "   names : String[*];\n" +
                "   prop() {$this.names + 'ok'} : String[1];\n" +
                "}", "COMPILATION error at [4:18-22]: Collection element must have a multiplicity [1] - Context:[Class 'test::A' Third Pass, Qualified Property prop, Applying plus], multiplicity:[*]");
        test("Class test::A\n" +
                "{\n" +
                "   names : String[0..1];\n" +
                "   prop() {$this.names + 'ok'} : String[1];\n" +
                "}", "COMPILATION error at [4:18-22]: Collection element must have a multiplicity [1] - Context:[Class 'test::A' Third Pass, Qualified Property prop, Applying plus], multiplicity:[0..1]");
    }

    @Test
    public void testConstraint()
    {
        test("Class test::A\n" +
                "[\n" +
                "   $this.names->isNotEmpty()\n" +
                "]\n" +
                "{\n" +
                "   names : String[*];\n" +
                "}");
        test("Class test::A\n" +
                "[\n" +
                "   $this.names->at(0)\n" +
                "]\n" +
                "{\n" +
                "   names : String[*];\n" +
                "}", "COMPILATION error at [3:17-18]: Constraint must be of type 'Boolean'");
    }

    @Test
    public void testPartialCompilationMultiplicityErrorInCollectionAndConstraint()
    {
        partialCompilationTest("Class test::A\n" +
                "{\n" +
                "   names : String[*];\n" +
                "   prop() {$this.names->at(0) + 'ok'} : String[1];\n" +
                "}");

        partialCompilationTest("Class test::A\n" +
                "{\n" +
                "   names : String[*];\n" +
                "   prop() {$this.names + 'ok'} : String[1];\n" +
                "}", Lists.fixedSize.with("COMPILATION error at [4:18-22]: Collection element must have a multiplicity [1] - Context:[Class 'test::A' Third Pass, Qualified Property prop, Applying plus], multiplicity:[*]"));

        partialCompilationTest("Class test::A\n" +
                "{\n" +
                "   names : String[0..1];\n" +
                "   prop() {$this.names + 'ok'} : String[1];\n" +
                "}\n" +
                "Class test::B\n" +
                "[\n" +
                "   $this.names->at(0)\n" +
                "]\n" +
                "{\n" +
                "   names : String[*];\n" +
                "}", Lists.fixedSize.with("COMPILATION error at [4:18-22]: Collection element must have a multiplicity [1] - Context:[Class 'test::A' Third Pass, Qualified Property prop, Applying plus], multiplicity:[0..1]", "COMPILATION error at [8:17-18]: Constraint must be of type 'Boolean'"));
    }

    @Test
    public void testReturnTypeErrorInQualifier()
    {
        test("Class test::A\n" +
                "{\n" +
                "   names : String[*];\n" +
                "   prop() {'1'} : String[1];\n" +
                "}");
        test("Class test::A\n" +
                "{\n" +
                "   names : String[*];\n" +
                "   prop() {1} : String[1];\n" +
                "}", "COMPILATION error at [4:12]: Error in derived property 'A.prop' - Type error: 'Integer' is not a subtype of 'String'");
    }

    @Test
    public void testReturnMultiplicityErrorInQualifier()
    {
        test("Class test::A\n" +
                "{\n" +
                "   names : String[*];\n" +
                "   prop() {['a','b']} : String[*];\n" +
                "}");
        test("Class test::A\n" +
                "{\n" +
                "   names : String[*];\n" +
                "   prop() {$this.names} : String[1];\n" +
                "}", "COMPILATION error at [4:18-22]: Error in derived property 'A.prop' - Multiplicity error: [1] doesn't subsume [*]");
    }

    @Test
    public void testEval1Param()
    {
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){ {a|$a+1}->eval(1);}:Integer[1];\n" +
                "}");
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){ {a|$a+'1'}->eval(1);}:Integer[1];\n" +
                "}", "COMPILATION error at [8:14-17]: Can't find a match for function 'plus(Any[2])'");
    }

    @Test
    public void testEval2Param()
    {
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){ {a,b|$a+$b}->eval(1,2);}:Integer[1];\n" +
                "}");
        test("Class test::A\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   z(){ {a,b|$a+$b}->eval(1,'a');}:Integer[1];\n" +
                "}", "COMPILATION error at [8:16-18]: Can't find a match for function 'plus(Any[2])'");
    }


    @Test
    public void testPropertyPostFunction()
    {
        test("Class test::Firm\n" +
                "{\n" +
                "   employees:test::Person[*];\n" +
                "   emp(){$this.employees->first().lastName}:String[0..1];\n" +
                "}\n" +
                "\n" +
                "Class test::Person\n" +
                "{\n" +
                "   lastName : String[1];\n" +
                "}");
    }

    @Test
    public void testUnknownFunction()
    {
        test("Class test::Person[$this.lastName->ranDoMFuncTion()]{lastName:String[1];}",
                "COMPILATION error at [1:36-49]: Function does not exist 'ranDoMFuncTion(String[1])'");
    }

    @Test
    public void testEnum()
    {
        test("Enum test::A\n" +
                "{\n" +
                "   A,B\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "   e:test::A[1];\n" +
                "   z(){ $this.e.name}:String[1];\n" +
                "}"
        );
    }

    @Test
    public void testFunction()
    {
        PureModel model = test("Class test::A\n" +
                "{\n" +
                "   s:String[1];\n" +
                "}\n" +
                "\n" +
                "function test::f(a:test::A[1]):String[1]\n" +
                "{\n" +
                "   $a.s;\n" +
                "}"
        ).getTwo();

        ConcreteFunctionDefinition<?> f = model.getConcreteFunctionDefinition("test::f_A_1__String_1_", null);
        Assert.assertNotNull(f);
        Assert.assertEquals("f_A_1__String_1_", f._name());
    }

    @Test
    public void testUserDefinedFunctionMatching()
    {
        test("Class test::A\n" +
                "{\n" +
                "   s:String[1];\n" +
                "}\n" +
                "function test::other(a:test::A[1]):String[1]\n" +
                "{\n" +
                "   test::f($a)\n" +
                "}\n" +
                "\n" +
                "function test::f(a:test::A[1]):String[1]\n" +
                "{\n" +
                "   $a.s;\n" +
                "}"
        );
    }

    @Test
    public void testUserDefinedFunctionMatchingError()
    {
        test("Class test::A\n" +
                        "{\n" +
                        "   s:String[1];\n" +
                        "}\n" +
                        "function test::other(a:test::A[1]):String[1]\n" +
                        "{\n" +
                        "   test::f('test')\n" +
                        "}\n" +
                        "\n" +
                        "function test::f(a:test::A[1]):String[1]\n" +
                        "{\n" +
                        "   $a.s;\n" +
                        "}",
                "COMPILATION error at [7:4-10]: Can't find a match for function 'test::f(String[1])'"
        );
    }

    @Test
    public void testUserDefinedFunctionMatchingInheritance()
    {
        test("Class test::B\n" +
                "{\n" +
                "   s:String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::A extends test::B\n" +
                "{\n" +
                "}\n" +
                "function test::other(a:test::A[1]):String[1]\n" +
                "{\n" +
                "   test::f($a)\n" +
                "}\n" +
                "\n" +
                "function test::f(a:test::B[1]):String[1]\n" +
                "{\n" +
                "   $a.s;\n" +
                "}"
        );
    }

    @Test
    public void testUserDefinedFunctionMatchingInheritanceError()
    {
        test("Class test::B\n" +
                "{\n" +
                "   s:String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::A extends test::B\n" +
                "{\n" +
                "}\n" +
                "function test::other(a:test::B[1]):String[1]\n" +
                "{\n" +
                "   test::f($a)\n" +
                "}\n" +
                "\n" +
                "function test::f(a:test::A[1]):String[1]\n" +
                "{\n" +
                "   $a.s;\n" +
                "}", "COMPILATION error at [11:4-10]: Can't find a match for function 'test::f(B[1])'"
        );
    }

    @Test
    public void testUserDefinedFunctionMatchingMultiplicity()
    {
        test("Class test::A\n" +
                "{\n" +
                "}\n" +
                "function test::other(a:test::A[1]):String[1]\n" +
                "{\n" +
                "   test::f($a)\n" +
                "}\n" +
                "\n" +
                "function test::f(a:test::A[*]):String[1]\n" +
                "{\n" +
                "   'bogus';\n" +
                "}"
        );
    }

    @Test
    public void testUserDefinedFunctionMatchingMultiplicityError()
    {
        test("Class test::A\n" +
                "{\n" +
                "}\n" +
                "function test::other(a:test::A[*]):String[1]\n" +
                "{\n" +
                "   test::f($a)\n" +
                "}\n" +
                "\n" +
                "function test::f(a:test::A[1]):String[1]\n" +
                "{\n" +
                "   'yo';\n" +
                "}", "COMPILATION error at [6:4-10]: Can't find a match for function 'test::f(A[*])'"
        );
    }

    @Test
    public void testFunctionReturnError()
    {
        test("Class test::A\n" +
                "{\n" +
                "   s:String[1];\n" +
                "}\n" +
                "\n" +
                "function test::f(a:test::A[1]):String[1]\n" +
                "{\n" +
                "   $a;\n" +
                "}", "COMPILATION error at [8:4-5]: Error in function 'test::f_A_1__String_1_' - Type error: 'test::A' is not a subtype of 'String'"
        );
    }

    @Test
    public void testFunctionReferenceBeforeFunctionDefinition()
    {
        test("function b::myFunction():String[1]\n" +
                "{\n" +
                "   z::otherFunction();\n" +
                "}\n" +
                "function z::otherFunction():String[1]\n" +
                "{\n" +
                "   'ok';\n" +
                "}"
        );
    }

    @Test
    public void testDeepfetch()
    {
        test("Class test::Person\n" +
                "{\n" +
                "   firstName:String[1];\n" +
                "   lastName:String[1];\n" +
                "}\n" +
                "Class test::Firm\n" +
                "{\n" +
                "   employees:test::Person[*];\n" +
                "}\n" +
                "Class test::Test\n" +
                "{\n" +
                "   x(){test::Person.all()->graphFetch(#{test::Person{firstName,lastName}}#);true;}:Boolean[1];\n" +
                "}");
    }

    @Test
    public void testDeepfetchPropertyError()
    {
        test("Class test::Person\n" +
                "{\n" +
                "   firstName:String[1];\n" +
                "   lastName:String[1];\n" +
                "}\n" +
                "Class test::Firm\n" +
                "{\n" +
                "   employees:test::Person[*];\n" +
                "}\n" +
                "Class test::Test\n" +
                "{\n" +
                // intentionally mess up the spacing in the deep fetch to see if we send the full string (with whitespaces) to the graph fetch tree parser
                "   x(){test::Person.all()->graphFetch(#{\n" +
                "       test::Person{\n" +
                "                first}}#);true;}:Boolean[1];\n" +
                "}\n", "COMPILATION error at [14:17-21]: Can't find property 'first' in [Person, Any]");
    }

    @Test
    public void testPartialCompilationDeepfetchPropertyErrorAndMissingProperty()
    {
        partialCompilationTest("import anything::*;\n" +
                "import test::*;\n" +
                "Class test::trial {\n" +
                "   name: ritual[*];\n" +
                "   anotherOne(){$this.name->toOne()}: ritual[1];\n" +
                "   \n" +
                "}\n" +
                "Class test::trial2 {\n" +
                "   name: trial[*];\n" +
                "   anotherOne(){$this.name->toOne().name2}: ritual[1];\n" +
                "   \n" +
                "}\n" +
                "\n" +
                "Enum anything::ritual {\n" +
                "   theGoodOne   \n" +
                "}\n" +
                "Class test::Person\n" +
                "{\n" +
                "   firstName:String[1];\n" +
                "   lastName:String[1];\n" +
                "}\n" +
                "Class test::Firm\n" +
                "{\n" +
                "   employees:test::Person[*];\n" +
                "}\n" +
                "Class test::Test\n" +
                "{\n" +
                // intentionally mess up the spacing in the deep fetch to see if we send the full string (with whitespaces) to the graph fetch tree parser
                "   x(){test::Person.all()->graphFetch(#{\n" +
                "       test::Person{\n" +
                "                first}}#);true;}:Boolean[1];\n" +
                "}\n", Lists.fixedSize.with("COMPILATION error at [10:37-41]: Can't find property 'name2' in class 'test::trial'", "COMPILATION error at [30:17-21]: Can't find property 'first' in [Person, Any]"));
    }

    @Test
    public void testDeepfetchTypeError()
    {
        test("Class test::Person\n" +
                "{\n" +
                "   firstName:String[1];\n" +
                "   lastName:String[1];\n" +
                "}\n" +
                "Class test::Firm\n" +
                "{\n" +
                "   employees:test::Person[*];\n" +
                "}\n" +
                "Class test::Test\n" +
                "{\n" +
                "   x(){test::Person.all()->graphFetch(#{test::Peron{first}}#);true;}:Boolean[1];\n" +
                "}\n", "COMPILATION error at [12:41-51]: Can't find class 'test::Peron'");
    }

    @Test
    public void testMatchMaxFunction()
    {
        test("function example::testMaxString():Any[0..1]\n" +
                "{\n" +
                "   ['string1', 'string2']->max();\n" +
                "}\n" +
                "function example::testMaxInteger():Any[0..1]\n" +
                "{\n" +
                "   [1,2]->max();\n" +
                "}\n" +
                "function example::testMaxFloat():Any[0..1]\n" +
                "{\n" +
                "   [1.0,2.0]->max();\n" +
                "}\n" +
                "function example::testMaxDate():Any[0..1]\n" +
                "{\n" +
                "   [%1999-01-01,%2000-01-01]->max();\n" +
                "}\n"
        );
    }

    @Test
    public void testMatchWithImport()
    {
        test("Class example::MyTest\n" +
                "{\n" +
                "p:String[1];\n" +
                "}\n" +
                "###Pure\n" +
                "import example::*;\n" +
                "function example::testMatch(test:MyTest[1]): MyTest[1]\n" +
                "{\n" +
                "  $test->match([ a:MyTest[1]|$a ]);\n" +
                "}");
    }

    @Test
    public void testAutoImports()
    {
        // TODO: we probably should test more types here based on the list of auto-imports
        test("Class {doc.doc = 'test'} test::doc {\n" +
                "\n" +
                "}\n" +
                "\n" +
                "Class test2::doc {\n" +
                "prop: Any[1];\n" +
                "}\n" +
                "\n");
        test("###Pure\n" +
                "import meta::pure::profiles::*;\n" +
                "import meta::pure::tests::model::simple::*;\n" +
                "Enum meta::pure::tests::model::simple::GeographicEntityType\n" +
                "{\n" +
                "    {doc.doc = 'A city, town, village, or other urban area.'} CITY,\n" +
                "    <<doc.deprecated>> COUNTRY,\n" +
                "    {doc.doc = 'Any geographic entity other than a city or country.'} REGION\n" +
                "}");
    }

    @Test
    public void testImportResolutionPrecedence()
    {
        // NOTE: notice that in PURE, we only validate the type of the reference during validation
        // so this test will also list `meta::pure::profiles::doc` in the list of matching resolved paths
        test("import test2::*;\n" +
                "import test::*;\n" +
                "\n" +
                "Class test::doc {}\n" +
                "Class test2::doc {}\n" +
                "\n" +
                "Class test::mewo {\n" +
                "   prop1: doc[1];\n" +
                "}", "COMPILATION error at [8:11-13]: Can't resolve element with path 'doc' - multiple matches found [test::doc, test2::doc]");
        test("import test2::*;\n" +
                "import test::*;\n" +
                "\n" +
                "Profile test::doc {}\n" +
                "Profile test2::doc {}\n" +
                "\n" +
                "Class <<doc.doc>> test::mewo {\n" +
                "}", "COMPILATION error at [7:9-11]: Can't resolve element with path 'doc' - multiple matches found [meta::pure::profiles::doc, test::doc, test2::doc]");
        // NOTE: since we disallow specifying having elements without a package
        // we can't test that primitive types and special types have precedence over
        // user defined elements at root package
    }

    @Test
    public void testDuplicatedImports()
    {
        // duplicated imports especially those that are similar to auto-imports are tolerated
        test("import meta::pure::profiles::*;\n" +
                "import meta::pure::profiles::*;\n" +
                "import meta::pure::profiles::*;\n" +
                "import random::path::*;\n" +
                "import random::path::*;\n" +
                "\n" +
                "Class {doc.doc = 'test'} test::doc {\n" +
                "\n" +
                "}\n" +
                "\n");
    }

    @Test
    public void testMissingProperty()
    {
        test("import anything::*;\n" +
                "Class test::trial {\n" +
                "   name: ritual[*];\n" +
                "   anotherOne(){$this.name2->toOne()}: ritual[1];\n" +
                "   \n" +
                "}\n" +
                "\n" +
                "Enum anything::ritual {\n" +
                "   theGoodOne   \n" +
                "}", "COMPILATION error at [4:23-27]: Can't find property 'name2' in class 'test::trial'");
        test("import anything::*;\n" +
                "import test::*;\n" +
                "Class test::trial {\n" +
                "   name: ritual[*];\n" +
                "   anotherOne(){$this.name->toOne()}: ritual[1];\n" +
                "   \n" +
                "}\n" +
                "Class test::trial2 {\n" +
                "   name: trial[*];\n" +
                "   anotherOne(){$this.name->toOne().name2}: ritual[1];\n" +
                "   \n" +
                "}\n" +
                "\n" +
                "Enum anything::ritual {\n" +
                "   theGoodOne   \n" +
                "}", "COMPILATION error at [10:37-41]: Can't find property 'name2' in class 'test::trial'");
    }

    @Test
    public void testMissingEnumValue()
    {
        test("import anything::*;\n" +
                "Class test::trial {\n" +
                "   name: ritual[*];\n" +
                "   anotherOne(){ritual.theGoodOne1}: ritual[1];\n" +
                "   \n" +
                "}\n" +
                "\n" +
                "Enum anything::ritual {\n" +
                "   theGoodOne   \n" +
                "}", "COMPILATION error at [4:24-34]: Can't find enum value 'theGoodOne1' in enumeration 'ritual'");
    }

    @Test
    public void testMissingStereotype()
    {
        test("import anything::*;\n" +
                "Class <<goes.businesstemporal>> {goes.doc = 'bla'} anything::A extends B, B\n" +
                "{\n" +
                "}\n" +
                "\n" +
                "Profile anything::goes\n" +
                "{\n" +
                "  stereotypes: [test];\n" +
                "  tags: [doc, todo];\n" +
                "}" +
                "\n", "COMPILATION error at [2:9-29]: Can't find stereotype 'businesstemporal' in profile 'goes'");
    }

    @Test
    public void testMissingTag()
    {
        test("import anything::*;\n" +
                "Class <<goes.test>> {goes.todo2 = 'bla'} anything::A extends B, B\n" +
                "{\n" +
                "}\n" +
                "\n" +
                "Profile anything::goes\n" +
                "{\n" +
                "  stereotypes: [test];\n" +
                "  tags: [doc, todo];\n" +
                "}" +
                "\n", "COMPILATION error at [2:27-31]: Can't find tag 'todo2' in profile 'goes'");
    }

    @Test
    public void testClassWithImport()
    {
        test("import anything::*;\n" +
                // Class stereotypes, tagged values and supertypes
                "Class <<goes.test>> {doc.doc = 'bla'} anything::A extends B\n" +
                "[\n" +
                "  $this.ok->toOne() == 1,\n" +
                "  constraint2: if($this.ok == 'ok', |true, |false)\n" +
                "]\n" +
                "{\n" +
                // simple property stereotypes, tagged values and type
                "  {goes.todo = 'bla'} name: B[*];\n" +
                "  {goes.doc = 'bla'} ok: A[1..2];\n" +
                "  dance: enumGoes[1..2];\n" +
                // derived property parameter and return type
                "  <<goes.test>> {goes.doc = 'bla'} xza(s:B[1]) {$s.z + 'ok'}:String[1];\n" +
                "  anotherOne() {$this.name->toOne()}: B[1];\n" +
                "}\n" +
                "\n" +
                "Class anything::B\n" +
                "{\n" +
                "  z: String[1];\n" +
                "}" +
                "Profile anything::goes\n" +
                "{\n" +
                "  stereotypes: [test];\n" +
                "  tags: [doc, todo];\n" +
                "}" +
                "Enum anything::enumGoes\n" +
                "{\n" +
                "  c\n" +
                "}\n" +
                "\n");
    }

    @Test
    public void testEnumerationWithImport()
    {
        test("import anything::*;\n" +
                "Profile anything::goes\n" +
                "{\n" +
                "  stereotypes: [test];\n" +
                "  tags: [doc, todo];\n" +
                "}" +
                // Enumeration tagged values and stereotypes
                "Enum <<goes.test>> {goes.doc = 'bla'} son::myEnum\n" +
                "{\n" +
                // Enum value tagged values and stereotypes
                "  <<goes.test>> {goes.doc = 'Tag Value for enum Value'} a,\n" +
                "  <<goes.test, goes.test>> {goes.doc = 'Tag Value for enum Value'} b,\n" +
                "  c\n" +
                "}\n" +
                "\n");
    }

    @Test
    public void testAssociationWithImport()
    {
        test("import anything::*;\n" +
                "Class anything::goes2\n" +
                "{\n" +
                "}\n" +
                "Profile anything::goes\n" +
                "{\n" +
                "  stereotypes: [test];\n" +
                "  tags: [doc, todo];\n" +
                "}\n" +
                "Association <<goes.test>> {goes.doc = 'Tag Value for assoc prop'} ahh::myAsso\n" +
                "{\n" +
                // `String` won't be valid here as we specifically look for a class
                "  <<goes.test>> {goes.doc = 'Tag Value for assoc prop'} a: String[1];\n" +
                "  <<goes.test>> {goes.doc = 'Tag Value for assoc prop'} b: goes2[1];\n" +
                "}\n" +
                "\n", "COMPILATION error at [12:3-69]: Can't find class 'String'");
        test("import anything::*;\n" +
                "Class anything::goes2\n" +
                "{\n" +
                "}\n" +
                "Profile anything::goes\n" +
                "{\n" +
                "  stereotypes: [test];\n" +
                "  tags: [doc, todo];\n" +
                "}" +
                // Association tagged values and stereotypes
                "Association <<goes.test>> {goes.doc = 'Tag Value for assoc prop'} ahh::myAsso\n" +
                "{\n" +
                // Association property tagged values, stereotypes, and type
                "  <<goes.test>> {goes.doc = 'Tag Value for assoc prop'} a: goes2[1];\n" +
                "  <<goes.test>> {goes.doc = 'Tag Value for assoc prop'} b: goes2[1];\n" +
                "}\n");
    }

    @Test
    public void testFunctionWithImport()
    {
        test("import anything::*;\n" +
                "Class anything::goes2\n" +
                "{\n" +
                "}\n" +
                "Profile anything::goes\n" +
                "{\n" +
                "  stereotypes: [test];\n" +
                "  tags: [doc, todo];\n" +
                "}" +
                "\n" +
                // Function stereotypes, tagged values, parameter types, and return type
                "function <<goes.test>> {goes.doc = 'Tag Value for assoc prop'} anything::f(s: goes2[1], s1:goes2[1]): goes2[1]\n" +
                "{\n" +
                "   f($s1, $s)\n" +
                "}");
    }


    @Test
    public void testBlockAny()
    {
        test("Class my::Class\n" +
                "{\n" +
                "}\n" +
                "\n" +
                "Association my::association\n" +
                "{\n" +
                "    toAny:Any[1];\n" +
                "    toClass:my::Class[1];\n" +
                "}", "COMPILATION error at [5:1-9:1]: Associations to Any are not allowed. Found in 'my::association'");

    }

    @Test
    public void testClassWithPath()
    {
        test("Class model::Person\n" +
                "{\n" +
                "    firstName: String[1];\n" +
                "    lastName: String[1];\n" +
                "}\n" +
                "\n" +
                "Class model::Firm\n" +
                "{\n" +
                "    legalName : String[1];\n" +
                "    employees: model::Person[*];\n" +
                "    employeesWithAddressNameSorted(){\n" +
                "       $this.employees->sortBy(#/model::Person/lastName#).lastName->joinStrings('')\n" +
                "    }:String[0..1];\n" +
                "}");
    }

    @Test
    public void testClassWithStrictTime()
    {
        test("Class apps::Trade\n" +
                "{\n" +
                "   time : StrictTime[1];\n" +
                "   testStrictTime(){\n" +
                "       $this.time == %9:12:22;\n" +
                "   } : Boolean[1];\n" +
                "}\n");
    }

    @Test
    public void testClassWithStrictTimeWithSubsec()
    {
        test("Class apps::Trade\n" +
                "{\n" +
                "   time : StrictTime[1];\n" +
                "   testStrictTime(){\n" +
                "       $this.time == %10:12:22.88;\n" +
                "   } : Boolean[1];\n" +
                "}\n");
    }

    @Test
    public void testClassWithStrictTimeWithParserError()
    {
        test("Class apps::Trade\n" +
                "{\n" +
                "   time : StrictTime[1];\n" +
                "   testStrictTime(){\n" +
                "       $this.time == %10:12:2b;\n" +
                "   } : Boolean[1];\n" +
                "}\n", "PARSER error at [5:30]: Unexpected token 'b'. Valid alternatives: ['&&', '||', '==', '!=', '->', '[', '.', ';', '+', '*', '-', '/', '<', '<=', '>', '>=']");

    }

    @Test
    public void testClassWithInvalidStrictTime()
    {
        Exception e1 = Assert.assertThrows(Exception.class, () -> test(
                "Class apps::Trade\n" +
                        "{\n" +
                        "   time : StrictTime[1];\n" +
                        "   testStrictTime(){\n" +
                        "       $this.time == %200:12:22.88;\n" +
                        "   } : Boolean[1];\n" +
                        "}\n"));
        Assert.assertEquals("Invalid hour: 200", e1.getMessage());

        Exception e2 = Assert.assertThrows(Exception.class, () -> test(
                "Class apps::Trade\n" +
                        "{\n" +
                        "   time : StrictTime[1];\n" +
                        "   testStrictTime(){\n" +
                        "       $this.time == %20:122:22.88;\n" +
                        "   } : Boolean[1];\n" +
                        "}\n"));
        Assert.assertEquals("Invalid minute: 122", e2.getMessage());

        Exception e3 = Assert.assertThrows(Exception.class, () -> test(
                "Class apps::Trade\n" +
                        "{\n" +
                        "   time : StrictTime[1];\n" +
                        "   testStrictTime(){\n" +
                        "       $this.time == %20:12:61.88;\n" +
                        "   } : Boolean[1];\n" +
                        "}\n"));
        Assert.assertEquals("Invalid second: 61", e3.getMessage());
    }

    @Test
    public void testReturnTypeErrorOfStrictTime()
    {
        test("Class test::A\n" +
                "{\n" +
                "   names : String[*];\n" +
                "   prop() {%20:20:20} : Date[1];\n" +
                "}", "COMPILATION error at [4:12-20]: Error in derived property 'A.prop' - Type error: 'StrictTime' is not a subtype of 'Date'");
    }

    @Test
    public void testFunctionWithDateTime()
    {
        test("function test::getDateTime(): DateTime[1]\n" +
                "{\n" +
                "   %2020-01-01T00:00:00.000\n" +
                "}\n");
    }

    @Test
    public void testClassWithStrictDate()
    {
        test("Class apps::Trade\n" +
                "{\n" +
                "   date : StrictDate[1];\n" +
                "   testStrictDate(){\n" +
                "       $this.date == %2020-01-01;\n" +
                "   } : Boolean[1];\n" +
                "}\n");
    }

    @Test
    public void testClassWithBusinessTemporalMilesoning()
    {
        Pair<PureModelContextData, PureModel> modelWithInput =
                test("Class apps::Employee \n" +
                        "{ \n" +
                        "  name: String[1]; \n" +
                        "  firm: apps::Firm[1]; \n" +
                        "}\n\n" +
                        "Class <<meta::pure::profiles::temporal.businesstemporal>> apps::Firm \n" +
                        "{ \n" +
                        "  name: String[1]; \n" +
                        "} \n" +
                        "Association apps::Employee_Firm \n" +
                        "{ \n" +
                        "  worksFor: apps::Firm[*]; \n" +
                        "  employs: apps::Employee[*]; \n" +
                        "} \n");
        PureModel model = modelWithInput.getTwo();
        Class<?> type = model.getClass("apps::Employee", SourceInformation.getUnknownSourceInformation());
        RichIterable<? extends Property<?, ?>> firmProperty = type._originalMilestonedProperties().select(p -> p.getName().equals("firm"));
        Assert.assertEquals("Missing firm property in _originalMilestonedProperties", 1, firmProperty.size());
        RichIterable<? extends Property<?, ?>> worksForProperty = type._originalMilestonedProperties().select(p -> p.getName().equals("worksFor"));
        Assert.assertEquals("Missing worksFor property in _originalMilestonedProperties", 1, worksForProperty.size());

        Association association = model.getAssociation("apps::Employee_Firm", SourceInformation.getUnknownSourceInformation());
        RichIterable<? extends Property<?, ?>> worksForPropertyFromAssoc = association._originalMilestonedProperties().select(p -> p.getName().equals("worksFor"));
        Assert.assertEquals("Missing worksFor property in _originalMilestonedProperties for association", 1, worksForPropertyFromAssoc.size());
    }

    @Test
    public void testClassesWithBusinessTemporalMilesoning()
    {
        Pair<PureModelContextData, PureModel> modelWithInput =
                test("Class <<meta::pure::profiles::temporal.businesstemporal>> apps::Employee \n" +
                        "{ \n" +
                        "  name: String[1]; \n" +
                        "  firm: apps::Firm[1]; \n" +
                        "}\n\n" +
                        "Class <<meta::pure::profiles::temporal.businesstemporal>> apps::Firm \n" +
                        "{ \n" +
                        "  name: String[1]; \n" +
                        "} \n" +
                        "Association apps::Employee_Firm \n" +
                        "{ \n" +
                        "  worksFor: apps::Firm[*]; \n" +
                        "  employs: apps::Employee[*]; \n" +
                        "} \n");
        PureModel model = modelWithInput.getTwo();
        Class<?> typeEmployee = model.getClass("apps::Employee", SourceInformation.getUnknownSourceInformation());
        RichIterable<? extends Property<?, ?>> firmProperty = typeEmployee._originalMilestonedProperties().select(p -> p.getName().equals("firm"));
        Assert.assertEquals("Missing firm property in _originalMilestonedProperties", 1, firmProperty.size());
        RichIterable<? extends Property<?, ?>> worksForProperty = typeEmployee._originalMilestonedProperties().select(p -> p.getName().equals("worksFor"));
        Assert.assertEquals("Missing worksFor property in _originalMilestonedProperties", 1, worksForProperty.size());

        Class<?> typeFirm = model.getClass("apps::Firm", SourceInformation.getUnknownSourceInformation());
        RichIterable<? extends Property<?, ?>> employsProperty = typeFirm._originalMilestonedProperties().select(p -> p.getName().equals("employs"));
        Assert.assertEquals("Missing employs property in _originalMilestonedProperties", 1, employsProperty.size());

        Association association = model.getAssociation("apps::Employee_Firm", SourceInformation.getUnknownSourceInformation());
        RichIterable<? extends Property<? extends Object, ? extends Object>> originalMilestonedProperties = association._originalMilestonedProperties();
        Assert.assertEquals("Expected 2 original milestoned properties, but found " + originalMilestonedProperties.size(), 2, originalMilestonedProperties.size());
        RichIterable<? extends Property<?, ?>> worksForPropertyFromAssoc = originalMilestonedProperties.select(p -> p.getName().equals("worksFor"));
        Assert.assertEquals("Missing worksFor property in _originalMilestonedProperties for association", 1, worksForPropertyFromAssoc.size());
        RichIterable<? extends Property<?, ?>> employsPropertyFromAssoc = originalMilestonedProperties.select(p -> p.getName().equals("employs"));
        Assert.assertEquals("Missing employs property in _originalMilestonedProperties for association", 1, employsPropertyFromAssoc.size());
    }

    @Test
    public void testClassesWithBusinessTemporalMilesoningWithDuplicatePropertyName()
    {
        Pair<PureModelContextData, PureModel> modelWithInput =
                test("Class <<meta::pure::profiles::temporal.businesstemporal>> apps::Employee \n" +
                        "{ \n" +
                        "  name: String[1]; \n" +
                        "  firm: apps::Firm[1]; \n" +
                        "}\n\n" +
                        "Class <<meta::pure::profiles::temporal.businesstemporal>> apps::Firm \n" +
                        "{ \n" +
                        "  name: String[1]; \n" +
                        "  employs: apps::Employee[1]; \n" +
                        "} \n" +
                        "Association apps::Employee_Firm \n" +
                        "{ \n" +
                        "  worksFor: apps::Firm[*]; \n" +
                        "  employs: apps::Employee[*]; \n" +
                        "} \n");
        PureModel model = modelWithInput.getTwo();
        Class<?> type = model.getClass("apps::Employee", SourceInformation.getUnknownSourceInformation());
        RichIterable<? extends Property<?, ?>> firmProperty = type._originalMilestonedProperties().select(p -> p.getName().equals("firm"));
        Assert.assertEquals("Missing firm property in _originalMilestonedProperties", 1, firmProperty.size());
        RichIterable<? extends Property<?, ?>> worksForProperty = type._originalMilestonedProperties().select(p -> p.getName().equals("worksFor"));
        Assert.assertEquals("Missing worksFor property in _originalMilestonedProperties", 1, worksForProperty.size());

        Association association = model.getAssociation("apps::Employee_Firm", SourceInformation.getUnknownSourceInformation());
        RichIterable<? extends Property<?, ?>> worksForPropertyFromAssoc = association._originalMilestonedProperties().select(p -> p.getName().equals("worksFor"));
        Assert.assertEquals("Missing worksFor property in _originalMilestonedProperties for association", 1, worksForPropertyFromAssoc.size());

        RichIterable<? extends Property<?, ?>> employsPropertyFromAssoc = association._originalMilestonedProperties().select(p -> p.getName().equals("employs"));
        Assert.assertEquals("Missing employs property in _originalMilestonedProperties for association", 1, employsPropertyFromAssoc.size());
    }

    public String getMilestoningModelWithDatePropagationAndInheritance()
    {
        return "###Pure\n" +
                "Class <<temporal.businesstemporal>> {doc.doc = 'Account class'} my::domainModel::migration::test::account::AccountValue\n" +
                "{\n" +
                "  value: String[1];\n" +
                "  productCollection: my::domainModel::migration::test::product::ProductCollection[*];\n" +
                "  getCategoryWithDatePropagation() {$this.productCollection(%2020-02-02).productType.category->joinStrings()}: String[1];\n" +
                "}\n" +
                "\n" +
                "Class <<temporal.businesstemporal>> {doc.doc = 'Product class'} my::domainModel::migration::test::product::ProductCollection extends my::domainModel::migration::test::product::Collections\n" +
                "{\n" +
                "  collectionName: String[1];\n" +
                "}\n" +
                "Class <<temporal.businesstemporal>> {doc.doc = 'Product class'} my::domainModel::migration::test::product::ProductType\n" +
                "{\n" +
                "   category: String[1];\n" +
                "}\n" +
                "Class <<temporal.businesstemporal>> {doc.doc = 'Product class'} my::domainModel::migration::test::product::Collections\n" +
                "{\n" +
                "  productType: my::domainModel::migration::test::product::ProductType[1];\n" +
                "}\n" +
                "Class my::domainModel::migration::test::product::Classification\n" +
                "{\n" +
                "  productType: my::domainModel::migration::test::product::ProductType[1];\n" +
                "}\n";
    }

    @Test
    public void testCompilationOfBusinessTemporalDatePropagationWithInheritance()
    {
        String grammar = getMilestoningModelWithDatePropagationAndInheritance();
        Pair<PureModelContextData, PureModel> modelWithInput = test(grammar);
        PureModel model = modelWithInput.getTwo();
        Class<?> collectionsClass = model.getClass("my::domainModel::migration::test::product::Collections", SourceInformation.getUnknownSourceInformation());
        RichIterable<? extends QualifiedProperty<?>> collectionsQPs = collectionsClass._qualifiedProperties();
        Assert.assertEquals(3, collectionsQPs.size());
        RichIterable<? extends QualifiedProperty<?>> singleDateQPWithArgAndNoArg = collectionsQPs.select(p -> p.getName().equals("productType"));
        Assert.assertEquals("Missing productType property for Class in my::domainModel::migration::test::product::Collections _qualifiedProperties", 2, singleDateQPWithArgAndNoArg.size());
        Assert.assertTrue("One of the productType properties for Class in my::domainModel::migration::test::product::Collections _qualifiedProperties should contain one argument for Date", ListIterate.anySatisfy(singleDateQPWithArgAndNoArg.toList(), qp -> ((FunctionType) (qp._classifierGenericType()._typeArguments().getFirst()._rawType()))._parameters().size() == 2));
        Assert.assertTrue("One of the productType properties for Class in my::domainModel::migration::test::product::Collections _qualifiedProperties should not contain one argument for Date", ListIterate.anySatisfy(singleDateQPWithArgAndNoArg.toList(), qp -> ((FunctionType) qp._classifierGenericType()._typeArguments().getFirst()._rawType())._parameters().size() == 1));
        Property<?, ?> edgePointProp = collectionsClass._properties().detect(p -> "productTypeAllVersions".equals(p.getName()));
        Assert.assertEquals("Multiplicity", edgePointProp._multiplicity().getClassifier().getName());
    }




    @Test
    public void testCompilationOfNonMilestonedClassToMilestonedClass()
    {
        String grammar = getMilestoningModelWithDatePropagationAndInheritance();
        test(grammar);
        Pair<PureModelContextData, PureModel> modelWithInput = test(grammar);
        PureModel model = modelWithInput.getTwo();
        Class<?> collectionsType = model.getClass("my::domainModel::migration::test::product::Classification", SourceInformation.getUnknownSourceInformation());
        RichIterable<? extends QualifiedProperty<?>> collectionsQPs = collectionsType._qualifiedProperties();
        Assert.assertEquals(2, collectionsQPs.size());
        Assert.assertEquals("Missing productType property for Class in my::domainModel::migration::test::product::Classification _qualifiedProperties", 1, collectionsQPs.select(p -> p.getName().equals("productType")).size());
        QualifiedProperty<?> singleDateQP = collectionsQPs.detect(p -> p.getName().equals("productType"));
        Assert.assertEquals("The productType property for Class in my::domainModel::migration::test::product::Classification _qualifiedProperties should contain one argument for Date", 2, ((FunctionType) singleDateQP._classifierGenericType()._typeArguments().getFirst()._rawType())._parameters().size());
    }

    @Test
    public void testMilestoningSimplePropertiesInNonMilestonedClassesAreNotRestricted()
    {
        String grammar = "###Pure\n" +
                "Class test::ProcessingDate{processingDate: Date[1];}" +
                "Class test::BusinessDate{businessDate: Date[1];}" +
                "Class test::ProcessingTemporalAddress\n" +
                "{\n" +
                "  processingDate: Date[1];\n" +
                "  processingDateComplex: test::ProcessingDate[1];" +
                "}\n" +
                "\n" +
                "Class test::BusinessTemporalAddress\n" +
                "{\n" +
                "  businessDate: Date[1];\n" +
                "  businessDateComplex: test::BusinessDate[1];\n" +
                "}\n" +
                "\n" +
                "Class test::BiTemporalAddress\n" +
                "{\n" +
                "  processingDate: Date[1];\n" +
                "  businessDate: Date[1];\n" +
                "  processingDateComplex: test::ProcessingDate[1];\n" +
                "  businessDateComplex: test::BusinessDate[1];\n" +
                "}\n";

        PureModel pm = test(grammar, null, Lists.mutable.with()).getTwo();
        RichIterable<? extends Property<?, ?>> processingTemporalAddressProperties = pm.getClass("test::ProcessingTemporalAddress")._properties();
        RichIterable<? extends Property<?, ?>> businessTemporalAddressProperties = pm.getClass("test::BusinessTemporalAddress")._properties();
        RichIterable<? extends Property<?, ?>> biTemporalAddressProperties = pm.getClass("test::BiTemporalAddress")._properties();

        Assert.assertEquals(2, processingTemporalAddressProperties.size());
        Assert.assertTrue(processingTemporalAddressProperties.allSatisfy(p -> Lists.immutable.with("processingDate", "processingDateComplex").contains(p.getName())));
        Assert.assertEquals(2, businessTemporalAddressProperties.size());
        Assert.assertTrue(businessTemporalAddressProperties.allSatisfy(p -> Lists.immutable.with("businessDate", "businessDateComplex").contains(p.getName())));
        Assert.assertEquals(4, biTemporalAddressProperties.size());
        Assert.assertTrue(biTemporalAddressProperties.allSatisfy(p -> Lists.immutable.with("processingDate", "processingDateComplex", "businessDate", "businessDateComplex").contains(p.getName())));
    }

    @Test
    public void testMilestoningPropertiesQualfiedExpressionForToOne()
    {
        String grammar = "###Pure\n" +
                "Class <<temporal.processingtemporal>> test::ProcessingTemporalAddress\n" +
                "{\n" +
                "  DunmmyProperty : String[1];\n" +
                "}\n" +
                "\n" +
                "Class <<temporal.businesstemporal>> test::BusinessTemporalAddress\n" +
                "{\n" +
                "  DunmmyProperty : String[1];\n" +
                "}\n" +
                "\n" +
                "Class <<temporal.bitemporal>> test::BiTemporalAddress\n" +
                "{\n" +
                "  DunmmyProperty : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::Person\n" +
                "{\n" +
                "  processingTemporalAddress1 : test::ProcessingTemporalAddress[1];\n" +
                "  businessTemporalAddress1 : test::BusinessTemporalAddress[1];\n" +
                "  biTemporalAddress1 : test::BiTemporalAddress[1];\n" +
                "  processingTemporalAddress2 : test::ProcessingTemporalAddress[0..1];\n" +
                "  businessTemporalAddress2 : test::BusinessTemporalAddress[0..1];\n" +
                "  biTemporalAddress2 : test::BiTemporalAddress[0..1];\n" +
                "  processingTemporalAddress3 : test::ProcessingTemporalAddress[*];\n" +
                "  businessTemporalAddress3 : test::BusinessTemporalAddress[*];\n" +
                "  biTemporalAddress3 : test::BiTemporalAddress[*];\n" +
                "}";
        PureModel pm = test(grammar).getTwo();

        RichIterable<? extends QualifiedProperty<?>> personQualifiedProperties = pm.getClass("test::Person")._qualifiedProperties();
        boolean checkQualifiedExpressionForprocessingTemporalAddress1 = checkQualifiedExpressionForToOneAsTopLevelFunctionExpression(personQualifiedProperties.detect(p -> p.getName().equals("processingTemporalAddress1")));
        boolean checkQualifiedExpressionForbusinessTemporalAddress1 = checkQualifiedExpressionForToOneAsTopLevelFunctionExpression(personQualifiedProperties.detect(p -> p.getName().equals("businessTemporalAddress1")));
        boolean checkQualifiedExpressionForbiTemporalAddress1 = checkQualifiedExpressionForToOneAsTopLevelFunctionExpression(personQualifiedProperties.detect(p -> p.getName().equals("biTemporalAddress1")));
        boolean checkQualifiedExpressionForprocessingTemporalAddress2 = checkQualifiedExpressionForToOneAsTopLevelFunctionExpression(personQualifiedProperties.detect(p -> p.getName().equals("processingTemporalAddress2")));
        boolean checkQualifiedExpressionForbusinessTemporalAddress2 = checkQualifiedExpressionForToOneAsTopLevelFunctionExpression(personQualifiedProperties.detect(p -> p.getName().equals("businessTemporalAddress2")));
        boolean checkQualifiedExpressionForbiTemporalAddress2 = checkQualifiedExpressionForToOneAsTopLevelFunctionExpression(personQualifiedProperties.detect(p -> p.getName().equals("biTemporalAddress2")));
        boolean checkQualifiedExpressionForprocessingTemporalAddress3 = checkQualifiedExpressionForToOneAsTopLevelFunctionExpression(personQualifiedProperties.detect(p -> p.getName().equals("processingTemporalAddress3")));
        boolean checkQualifiedExpressionForbusinessTemporalAddress3 = checkQualifiedExpressionForToOneAsTopLevelFunctionExpression(personQualifiedProperties.detect(p -> p.getName().equals("businessTemporalAddress3")));
        boolean checkQualifiedExpressionForbiTemporalAddress3 = checkQualifiedExpressionForToOneAsTopLevelFunctionExpression(personQualifiedProperties.detect(p -> p.getName().equals("biTemporalAddress3")));
        Assert.assertTrue("Qualfied expression generated for processingTemporalAddress should have toOne() as topLevel functionExpression", checkQualifiedExpressionForprocessingTemporalAddress1);
        Assert.assertTrue("Qualfied expression generated for BusinessTemporalAddress should have toOne() as topLevel functionExpression", checkQualifiedExpressionForbusinessTemporalAddress1);
        Assert.assertTrue("Qualfied expression generated for BiTemporalAddress should have toOne() as topLevel functionExpression", checkQualifiedExpressionForbiTemporalAddress1);
        Assert.assertFalse("Qualfied expression generated for processingTemporalAddress should not have toOne() as topLevel functionExpression", checkQualifiedExpressionForprocessingTemporalAddress2);
        Assert.assertFalse("Qualfied expression generated for BusinessTemporalAddress should not have toOne() as topLevel functionExpression", checkQualifiedExpressionForbusinessTemporalAddress2);
        Assert.assertFalse("Qualfied expression generated for BiTemporalAddress should not have toOne() as topLevel functionExpression", checkQualifiedExpressionForbiTemporalAddress2);
        Assert.assertFalse("Qualfied expression generated for processingTemporalAddress should not have toOne() as topLevel functionExpression", checkQualifiedExpressionForprocessingTemporalAddress3);
        Assert.assertFalse("Qualfied expression generated for BusinessTemporalAddress should not have toOne() as topLevel functionExpression", checkQualifiedExpressionForbusinessTemporalAddress3);
        Assert.assertFalse("Qualfied expression generated for BiTemporalAddress should not have toOne() as topLevel functionExpression", checkQualifiedExpressionForbiTemporalAddress3);
    }

    private boolean checkQualifiedExpressionForToOneAsTopLevelFunctionExpression(QualifiedProperty<?> generatedMilestoningClassQualifiedProperty)
    {
        FunctionExpression topLevelExpression = ((FunctionExpression) generatedMilestoningClassQualifiedProperty._expressionSequence().getFirst());
        return topLevelExpression._func()._functionName().equals("toOne");
    }

    @Test
    public void testMilestoningSimplePropertiesAreNotOverriddenByUserProperties()
    {
        String grammar = "###Pure\n" +
                "Class <<temporal.processingtemporal>> test::ProcessingTemporalAddress\n" +
                "{\n" +
                "  processingDate: Date[1];\n" +
                "}\n" +
                "\n" +
                "Class <<temporal.businesstemporal>> test::BusinessTemporalAddress\n" +
                "{\n" +
                "  businessDate: Date[1];\n" +
                "}\n" +
                "\n" +
                "Class <<temporal.bitemporal>> test::BiTemporalAddress\n" +
                "{\n" +
                "  processingDate: Date[1];\n" +
                "  businessDate: Date[1];\n" +
                "}\n" +
                "\n" +
                "Class test::Person\n" +
                "{\n" +
                "  processingTemporalAddress : test::ProcessingTemporalAddress[1];\n" +
                "  businessTemporalAddress : test::BusinessTemporalAddress[1];\n" +
                "  biTemporalAddress : test::BiTemporalAddress[1];\n" +
                "}";
        PureModel pm = test(grammar, null, Lists.mutable.with(
                "COMPILATION error at [2:1-5:1]: Class test::ProcessingTemporalAddress has temporal specification: [processingtemporal] properties: [processingDate] are reserved and should not be explicit in the Model",
                "COMPILATION error at [7:1-10:1]: Class test::BusinessTemporalAddress has temporal specification: [businesstemporal] properties: [businessDate] are reserved and should not be explicit in the Model",
                "COMPILATION error at [12:1-16:1]: Class test::BiTemporalAddress has temporal specification: [bitemporal] properties: [processingDate, businessDate] are reserved and should not be explicit in the Model"

        )).getTwo();

        java.util.function.Function<Property<?, ?>, Boolean> isGeneratedMilestoningProperty = p -> p._stereotypes().anySatisfy(s -> s._value().equals(Milestoning.GeneratedMilestoningStereotype.generatedmilestoningdateproperty.name()));

        Property<?, ?> processingDateProperty = pm.getClass("test::ProcessingTemporalAddress")._properties().select(p -> p.getName().equals("processingDate")).getOnly();
        Property<?, ?> businessDateProperty = pm.getClass("test::BusinessTemporalAddress")._properties().select(p -> p.getName().equals("businessDate")).getOnly();

        Assert.assertTrue(isGeneratedMilestoningProperty.apply(processingDateProperty));
        Assert.assertTrue(isGeneratedMilestoningProperty.apply(businessDateProperty));
        RichIterable<? extends QualifiedProperty<?>> personQualifiedProperties = pm.getClass("test::Person")._qualifiedProperties();

        boolean classProcessingDatePropertyIsUsedInMiletoningExpression = generatedMilestoningQualifiedPropertyUsesGeneratedMilestoningProperty(processingDateProperty, personQualifiedProperties.detect(p -> p.getName().equals("processingTemporalAddress")));
        boolean classBusinessPropertyIsUsedInMiletoningExpression = generatedMilestoningQualifiedPropertyUsesGeneratedMilestoningProperty(businessDateProperty, personQualifiedProperties.detect(p -> p.getName().equals("businessTemporalAddress")));
        Assert.assertTrue("Class generated milestoning processingDate property should be used in the generated milestoning expression", classProcessingDatePropertyIsUsedInMiletoningExpression);
        Assert.assertTrue("Class generated milestoning businessDate property should be used in the generated milestoning expression", classBusinessPropertyIsUsedInMiletoningExpression);

        RichIterable<? extends Property<?, ?>> biTemporalMilestoningDateProperties = pm.getClass("test::BiTemporalAddress")._properties().select(p -> Lists.fixedSize.with("businessDate", "processingDate").contains(p.getName()));
        Assert.assertTrue(biTemporalMilestoningDateProperties.size() == 2 && biTemporalMilestoningDateProperties.anySatisfy(p -> p.getName().equals("businessDate")) && biTemporalMilestoningDateProperties.anySatisfy(p -> p.getName().equals("processingDate")));
        Assert.assertTrue(biTemporalMilestoningDateProperties.allSatisfy(isGeneratedMilestoningProperty::apply));
        boolean classProcessingDatePropertyIsUsedInBiTemporalMiletoningExpression = generatedMilestoningQualifiedPropertyUsesGeneratedMilestoningProperty(biTemporalMilestoningDateProperties.detect(p -> p.getName().equals("processingDate")), personQualifiedProperties.detect(p -> p.getName().equals("biTemporalAddress")));
        boolean classBusinessPropertyIsUsedInBiTemporalMiletoningExpression = generatedMilestoningQualifiedPropertyUsesGeneratedMilestoningProperty(biTemporalMilestoningDateProperties.detect(p -> p.getName().equals("businessDate")), personQualifiedProperties.detect(p -> p.getName().equals("biTemporalAddress")));
        Assert.assertTrue("Class generated milestoning processingDate property should be used in the generated milestoning expression", classProcessingDatePropertyIsUsedInBiTemporalMiletoningExpression);
        Assert.assertTrue("Class generated milestoning businessDate property should be used in the generated milestoning expression", classBusinessPropertyIsUsedInBiTemporalMiletoningExpression);
    }

    private boolean generatedMilestoningQualifiedPropertyUsesGeneratedMilestoningProperty(Property<?, ?> generatedMilestoningClassSimpleProperty, QualifiedProperty<?> generatedMilestoningClassQualifiedProperty)
    {
        FunctionExpression topLevelExpression = ((FunctionExpression) generatedMilestoningClassQualifiedProperty._expressionSequence().getFirst());
        FunctionExpression simplifiedExpression;
        if (topLevelExpression._func()._functionName().equals("toOne"))
        {
            simplifiedExpression = (FunctionExpression) ((LambdaFunction<?>) ((InstanceValue) ((FunctionExpression) (topLevelExpression._parametersValues().toList().get(0)))._parametersValues().toList().get(1))._values().getOnly())._expressionSequence().getOnly();
        }
        else
        {
            simplifiedExpression = (FunctionExpression) ((LambdaFunction<?>) ((InstanceValue) topLevelExpression._parametersValues().toList().get(1))._values().getOnly())._expressionSequence().getOnly();
        }
        if (simplifiedExpression._func()._functionName().equals("and"))
        {
            int idx = generatedMilestoningClassSimpleProperty.getName().equals("processingDate") ? 0 : 1;
            simplifiedExpression = (FunctionExpression) simplifiedExpression._parametersValues().toList().get(idx);
        }
        Property<?, ?> filterMilestoningDateProperty = (Property<?, ?>) ((FunctionExpression) simplifiedExpression._parametersValues().toList().get(0))._func();
        return generatedMilestoningClassSimpleProperty.equals(filterMilestoningDateProperty);
    }

    @Test
    public void testFunctionNamingUsingUnderStore()
    {
        String grammar =
                "###Pure\n" +
                        "Class <<temporal.businesstemporal>> {doc.doc = 'Account class'} my::domainModel::migration::test::account::AccountValue\n" +
                        "{\n" +
                        "  value: String[1];\n" +
                        "  productCollecton: my::domainModel::migration::test::product::ProductCollection[*];\n" +
                        "  getCategory() {$this->my::domainModel::migration::test::product::my_helper_function('tt')}: String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.businesstemporal>> {doc.doc = 'Product class'} my::domainModel::migration::test::product::ProductCollection\n" +
                        "{\n" +
                        "  collectionName: String[1];\n" +
                        "  type: my::domainModel::migration::test::product::ProductType[1];\n" +
                        "}\n" +
                        "Class <<temporal.businesstemporal>> {doc.doc = 'Product class'} my::domainModel::migration::test::product::ProductType\n" +
                        "{\n" +
                        "   category: String[1];\n" +
                        "}\n" +
                        "function my::domainModel::migration::test::product::my_helper_function(acc: my::domainModel::migration::test::account::AccountValue[1], str: String[1]):String[1]\n" +
                        "{\n" +
                        "  $acc.productCollecton(%2020-02-02).type.category->joinStrings();\n" +
                        "}\n";

        test(grammar);
    }

    @Test
    public void testLambdaReturnTypes()
    {
        Pair<PureModelContextData, PureModel> modelWithInput =
                test("###Pure \n" +
                        " \n" +
                        "Class main::Person \n" +
                        "{ \n" +
                        "    name : String[1]; \n" +
                        "} \n" +
                        " \n" +
                        "function main::walkTree(zero: String[2..*], people: main::Person[*]): String[*] \n" +
                        "{ \n" +
                        "    $people->fold({p,a|$a->concatenate($p.name)}, $zero); \n" +
                        "} \n");
        PureModel pureModel = modelWithInput.getTwo();

        String WALK_TREE = "main::walkTree_String_$2_MANY$__Person_MANY__String_MANY_";

        ConcreteFunctionDefinition<?> walkTree = pureModel.getConcreteFunctionDefinition(WALK_TREE, null);
        SimpleFunctionExpression fold = (SimpleFunctionExpression) walkTree._expressionSequence().getFirst();
        InstanceValue iv = (InstanceValue) fold._parametersValues().toList().get(1);
        SimpleFunctionExpression concat = (SimpleFunctionExpression) ((LambdaFunction<?>) iv._values().getFirst())._expressionSequence().getFirst();
        Assert.assertEquals(pureModel.getType("String"), fold._genericType()._rawType());
        Assert.assertEquals(pureModel.getType("String"), concat._genericType()._rawType());

        Multiplicity accumMul = concat._parametersValues().getFirst()._multiplicity();
        Assert.assertEquals(2L, accumMul._lowerBound()._value().longValue());
        Assert.assertNull(accumMul._upperBound()._value());
    }

    @Test
    public void testCompilationOfLambdaWithBiTemporalClass()
    {
        test("###Pure \n" +
                " \n" +
                "Class <<temporal.bitemporal>> main::Person \n" +
                "{ \n" +
                "    name : String[1]; \n" +
                "    firm : main::Firm[1]; \n" +
                "} \n" +
                " \n" +
                "Class <<temporal.bitemporal>> main::Firm \n" +
                "{ \n" +
                "    name : String[1]; \n" +
                "} \n" +
                " \n" +
                "function main::walkTree(): main::Person[*] \n" +
                "{ \n" +
                "    main::Person.all(%2020-12-12, %2020-12-13)  \n" +
                "} \n" +
                " \n" +
                "function main::walkTree1(): main::Person[*] \n" +
                "{ \n" +
                "    main::Person.all(%latest, %latest)  \n" +
                "} \n" +
                " \n" +
                "function main::walkTree2(): main::Person[*] \n" +
                "{ \n" +
                "    main::Person.all(%latest, %2020-12-12)  \n" +
                "} \n" +
                " \n" +
                "function main::walkTree3(): main::Firm[*] \n" +
                "{ \n" +
                "    main::Person.all(%2020-12-12, %2020-12-13).firm(%latest, %latest)  \n" +
                "} \n");
    }

    @Test
    public void testCompilationForEmbeddedPureExtension()
    {
        test("function x::f(): Any[*]\n" +
                "{\n" +
                "   let x = #Test{My random Parser #Test{ OK OK } Yo}#;" +
                "}\n");
    }

    @Test
    public void testGraphFetchTreeWithQualifierCompilation()
    {
        String code = "Class test::Firm\n" +
                "{\n" +
                "  legalName: String[1];\n" +
                "  employees: test::Person[*];\n" +
                "\n" +
                "  employeeCount() {$this.employees->size()}: Integer[1];\n" +
                "  employeesByFirstName(firstNames: String[*]) {$this.employees->filter(e | $e.firstName->in($firstNames))}: test::Person[*];\n" +
                "  employeesByFirstNameAndCity(firstNames: String[*], cities: String[*]) {$this.employees->filter(e | $e.firstName->in($firstNames) && $e.city->in($cities))}: test::Person[*];\n" +
                "}\n" +
                "\n" +
                "Class test::Person\n" +
                "{\n" +
                "  firstName: String[1];\n" +
                "  lastName: String[1];\n" +
                "  city: String[1]; \n" +
                "}\n" +
                "\n" +
                "function my::test():Any[*]\n" +
                "{\n" +
                "  #{\n" +
                "    test::Firm {\n" +
                "      legalName,\n" +
                "      employeeCount(),\n" +
                "      employeesByFirstName([]) {\n" +
                "        firstName,\n" +
                "        lastName\n" +
                "      },\n" +
                "      employeesByFirstName('Peter') {\n" +
                "        firstName,\n" +
                "        lastName\n" +
                "      },\n" +
                "      employeesByFirstName(['Peter']) {\n" +
                "        firstName,\n" +
                "        lastName\n" +
                "      },\n" +
                "      employeesByFirstName(['Peter', 'John']) {\n" +
                "        firstName,\n" +
                "        lastName\n" +
                "      },\n" +
                "      employeesByFirstNameAndCity(['Peter', 'John'], ['New York']) {\n" +
                "        firstName,\n" +
                "        lastName\n" +
                "      }\n" +
                "    }\n" +
                "  }#\n" +
                "}\n";

        test(code);
    }

    @Test
    public void testCompilationForGraphFetchTreeWithSubTypeTreeAtRootLevel()
    {
        test("Class test::Address\n" +
                "{\n" +
                "  zipCode : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::Street extends test::Address\n" +
                "{\n" +
                "  street: String[1];\n" +
                "}\n" +
                "\n" +
                "function my::test():Any[*]\n" +
                "{\n" +
                "  #{\n" +
                "    test::Address {\n" +
                "      zipCode,\n" +
                "      ->subType(@test::Street) {\n" +
                "        street\n" +
                "      }\n" +
                "    }\n" +
                "  }#\n" +
                "}\n"
        );

        test("Class test::Address\n" +
                "{\n" +
                "  zipCode : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::Street\n" +
                "{\n" +
                "  street: String[1];\n" +
                "}\n" +
                "\n" +
                "function my::test():Any[*]\n" +
                "{\n" +
                "  #{\n" +
                "    test::Address {\n" +
                "      zipCode,\n" +
                "      ->subType(@test::Street) {\n" +
                "        street\n" +
                "      }\n" +
                "    }\n" +
                "  }#\n" +
                "}\n", "COMPILATION error at [16:18-29]: The type Street is not a subtype of Address"
        );

        test("Class test::Address\n" +
                "{\n" +
                "  zipCode : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::Street extends test::Address\n" +
                "{\n" +
                "  street: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::City extends test::Address\n" +
                "{\n" +
                "  name: String[1];\n" +
                "}\n" +
                "\n" +
                "function my::test():Any[*]\n" +
                "{\n" +
                "  #{\n" +
                "    test::Address {\n" +
                "      zipCode,\n" +
                "      ->subType(@test::Street) {\n" +
                "        street\n" +
                "      },\n" +
                "      ->subType(@test::City) {\n" +
                "        name\n" +
                "      }\n" +
                "    }\n" +
                "  }#\n" +
                "}\n"
        );

        test("Class test::Address\n" +
                "{\n" +
                "  zipCode : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::Street extends test::Address\n" +
                "{\n" +
                "  street: String[1];\n" +
                "}\n" +
                "\n" +
                "function my::test():Any[*]\n" +
                "{\n" +
                "  #{\n" +
                "    test::Address {\n" +
                "      zipCode,\n" +
                "      ->subType(@test::Street) {\n" +
                "        street\n" +
                "      },\n" +
                "      ->subType(@test::Street) {\n" +
                "        street\n" +
                "      }\n" +
                "    }\n" +
                "  }#\n" +
                "}\n", "COMPILATION error at [19:18-29]: There are multiple subTypeTrees having subType test::Street, Only one is allowed"
        );

        test("Class test::Address\n" +
                "{\n" +
                "  zipCode : String[1];\n" +
                "  name:  String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::Street extends test::Address\n" +
                "{\n" +
                "  street: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::City extends test::Address\n" +
                "{\n" +
                "  capital: String[1];\n" +
                "}\n" +
                "\n" +
                "function my::test():Any[*]\n" +
                "{\n" +
                "  #{\n" +
                "    test::Address {\n" +
                "      zipCode,\n" +
                "        name,\n" +
                "      ->subType(@test::Street) {\n" +
                "        street\n" +
                "      },\n" +
                "      ->subType(@test::City) {\n" +
                "        name\n" +
                "      }\n" +
                "    }\n" +
                "  }#\n" +
                "}\n", "COMPILATION error at [26:18-27]: Property \"name\" is present at root level hence should not be specified at subType level"
        );

        test("Class test::Address\n" +
                "{\n" +
                "  zipCode : String[1];\n" +
                "  name:  String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::Street extends test::Address\n" +
                "{\n" +
                "  street: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::City extends test::Address\n" +
                "{\n" +
                "  capital: String[1];\n" +
                "}\n" +
                "\n" +
                "function my::test():Any[*]\n" +
                "{\n" +
                "  #{\n" +
                "    test::Address {\n" +
                "      zipCode,\n" +
                "        'alias':name,\n" +
                "      ->subType(@test::Street) {\n" +
                "        street\n" +
                "      },\n" +
                "      ->subType(@test::City) {\n" +
                "        'alias':capital\n" +
                "      }\n" +
                "    }\n" +
                "  }#\n" +
                "}\n", "COMPILATION error at [26:18-27]: Property \"alias\" is present at root level hence should not be specified at subType level"
        );

        test("Class test::Address\n" +
                "{\n" +
                "  zipCode : String[1];\n" +
                "  name:  String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::Street extends test::Address\n" +
                "{\n" +
                "  street: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::City extends test::Address\n" +
                "{\n" +
                "  capital: String[1];\n" +
                "}\n" +
                "\n" +
                "function my::test():Any[*]\n" +
                "{\n" +
                "  #{\n" +
                "    test::Address {\n" +
                "      zipCode,\n" +
                "        name,\n" +
                "      ->subType(@test::Street) {\n" +
                "        street\n" +
                "      },\n" +
                "      ->subType(@test::City) {\n" +
                "        'cityName' : name\n" +
                "      }\n" +
                "    }\n" +
                "  }#\n" +
                "}\n"
        );

        test("Class test::Address\n" +
                "{\n" +
                "  zipCode : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::City extends test::Address\n" +
                "{\n" +
                "  cityName: String[1];\n" +
                "}\n" +
                "\n" +
                "function my::test():Any[*]\n" +
                "{\n" +
                "  #{\n" +
                "    test::Address {\n" +
                "      ->subType(@test::City) {\n" +
                "         fakeProperty\n" +
                "      }\n" +
                "    }\n" +
                "  }#\n" +
                "}\n", "COMPILATION error at [16:10-21]: Can't find property 'fakeProperty' in [City, Address, Any]"
        );
    }

    @Test
    public void testCompilationOfBinaryType()
    {
        // This is added to test legacy Binary primitive type usages
        test("Class demo::Class\n" +
                "{\n" +
                "   binary: Binary[1];\n" +
                "}\n");
    }

    @Test
    public void testCompilationOfRelationStoreAccessor()
    {
        Exception e = Assert.assertThrows(Exception.class, () -> test(
                "function my::func():Any[*]\n" +
                        "{\n" +
                        "   #>{my::Store}#->filter(c|$c.val);\n" +
                        "}\n"));
        Assert.assertEquals("The store 'my::Store' can't be found.", e.getMessage());
    }

    @Test
    public void testCompilationOfNativeFunctions()
    {
        // Compilation of String Native Functions

        // EncodeUrl
        test(wrapExpressionWithFunctionGrammar("encodeUrl('dummy')"));
        test(wrapExpressionWithFunctionGrammar("encodeUrl('dummy', 'ascii')"));

        // DecodeUrl
        test(wrapExpressionWithFunctionGrammar("decodeUrl('dummy')"));
        test(wrapExpressionWithFunctionGrammar("decodeUrl('dummy', 'ascii')"));
    }

    private String wrapExpressionWithFunctionGrammar(String exp)
    {
        return "function test::testStringNativeFunctionCompilationFromGrammar():Any[*]\n" +
                "{\n" +
                exp +
                "}\n";
    }

    @Test
    public void testFunctionMatchingError_MismatchOnType()
    {
        test("function my::functionParent(val: Integer[1]):String[1]\n" +
                "{\n" +
                "    between($val, %2020-01-01, %2021-01-01);\n" +
                "}",
        "COMPILATION error at [3:5-11]: Can't find a match for function 'between(Integer[1],StrictDate[1],StrictDate[1])'.\n" +
                "Functions that can match if parameter types or multiplicities are changed:\n" +
                "\t\tbetween(StrictDate[0..1],StrictDate[0..1],StrictDate[0..1]):Boolean[1]\n" +
                "\t\tbetween(DateTime[0..1],DateTime[0..1],DateTime[0..1]):Boolean[1]\n" +
                "\t\tbetween(Number[0..1],Number[0..1],Number[0..1]):Boolean[1]\n" +
                "\t\tbetween(String[0..1],String[0..1],String[0..1]):Boolean[1]");
    }

    @Test
    public void testFunctionMatchingError_WrongNumberOfParameters()
    {
        test("function my::functionParent():String[1]\n" +
                        "{\n" +
                        "    between(1, 2);\n" +
                        "}",
                "COMPILATION error at [3:5-11]: Can't find a match for function 'between(Integer[1],Integer[1])'.\n" +
                        "Functions that can match if number of parameters are changed:\n" +
                        "\t\tbetween(StrictDate[0..1],StrictDate[0..1],StrictDate[0..1]):Boolean[1]\n" +
                        "\t\tbetween(DateTime[0..1],DateTime[0..1],DateTime[0..1]):Boolean[1]\n" +
                        "\t\tbetween(Number[0..1],Number[0..1],Number[0..1]):Boolean[1]\n" +
                        "\t\tbetween(String[0..1],String[0..1],String[0..1]):Boolean[1]");
    }

    @Test
    public void testFunctionMatchingError_WrongMultiplicity()
    {
        test("function my::functionParent(val: Integer[0..1]):String[1]\n" +
                        "{\n" +
                        "    abs($val);\n" +
                        "}",
                "COMPILATION error at [3:5-7]: Can't find a match for function 'abs(Integer[0..1])'.\n" +
                        "Functions that can match if parameter types or multiplicities are changed:\n" +
                        "\t\tabs(Float[1]):Float[1]\n" +
                        "\t\tabs(Integer[1]):Integer[1]\n" +
                        "\t\tabs(Decimal[1]):Decimal[1]\n" +
                        "\t\tabs(Number[1]):Number[1]");
    }
}
