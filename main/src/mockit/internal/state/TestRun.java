/*
 * Copyright (c) 2006-2015 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.state;

import org.jetbrains.annotations.*;

import mockit.internal.mockups.*;
import mockit.internal.expectations.*;
import mockit.internal.expectations.mocking.*;
import mockit.internal.util.*;

/**
 * A singleton which stores several data structures which in turn hold global state for individual test methods, test
 * classes, and for the test run as a whole.
 */
public final class TestRun
{
   private static final TestRun INSTANCE = new TestRun();
   private TestRun() {}

   // Fields with global state ////////////////////////////////////////////////////////////////////////////////////////

   private static final ThreadLocal<Integer> noMockingCount = new ThreadLocal<Integer>() {
      @Override protected Integer initialValue() { return 0; }
      @Override public void set(Integer valueToAdd) { super.set(get() + valueToAdd); }
   };

   // Used only by the Coverage tool:
   private int testId;

   @Nullable private Class<?> currentTestClass;
   @Nullable private Object currentTestInstance;
   @Nullable private FieldTypeRedefinitions fieldTypeRedefinitions;

   @NotNull private final MockFixture mockFixture = new MockFixture();

   @NotNull private final ExecutingTest executingTest = new ExecutingTest();
   @NotNull private final MockClasses mockClasses = new MockClasses();

   // Static "getters" for global state ///////////////////////////////////////////////////////////////////////////////

   public static boolean isInsideNoMockingZone() { return noMockingCount.get() > 0; }

   @Nullable public static Class<?> getCurrentTestClass() { return INSTANCE.currentTestClass; }

   @Nullable public static Object getCurrentTestInstance() { return INSTANCE.currentTestInstance; }

   @SuppressWarnings("unused")
   public static int getTestId() { return INSTANCE.testId; }

   @Nullable
   public static FieldTypeRedefinitions getFieldTypeRedefinitions() { return INSTANCE.fieldTypeRedefinitions; }

   @NotNull public static MockFixture mockFixture() { return INSTANCE.mockFixture; }

   @NotNull public static ExecutingTest getExecutingTest() { return INSTANCE.executingTest; }

   @Nullable public static RecordAndReplayExecution getRecordAndReplayForRunningTest()
   {
      return INSTANCE.executingTest.getCurrentRecordAndReplay();
   }

   @NotNull public static RecordAndReplayExecution getOrCreateRecordAndReplayForRunningTest()
   {
      return INSTANCE.executingTest.getOrCreateRecordAndReplay();
   }

   @NotNull public static RecordAndReplayExecution getRecordAndReplayForVerifications()
   {
      if (INSTANCE.fieldTypeRedefinitions == null) {
         IllegalStateException failure = new IllegalStateException("Invalid place to verify expectations");
         StackTrace.filterStackTrace(failure);
         throw failure;
      }

      return INSTANCE.executingTest.getRecordAndReplayForVerifications();
   }

   @NotNull public static MockClasses getMockClasses() { return INSTANCE.mockClasses; }
   @NotNull public static MockStates getMockStates() { return INSTANCE.mockClasses.mockStates; }

   // Static "mutators" for global state //////////////////////////////////////////////////////////////////////////////

   public static void setCurrentTestClass(@Nullable Class<?> testClass)
   {
      INSTANCE.currentTestClass = testClass;
   }

   public static void prepareForNextTest()
   {
      INSTANCE.testId++;
      INSTANCE.executingTest.setRecordAndReplay(null);
   }

   public static void enterNoMockingZone() { noMockingCount.set(1); }
   public static void exitNoMockingZone()  { noMockingCount.set(-1); }
   public static void clearNoMockingZone() { noMockingCount.remove(); }

   public static void setRunningIndividualTest(@Nullable Object testInstance)
   {
      INSTANCE.currentTestInstance = testInstance;
   }

   public static void setFieldTypeRedefinitions(@Nullable FieldTypeRedefinitions redefinitions)
   {
      INSTANCE.fieldTypeRedefinitions = redefinitions;
   }

   public static void finishCurrentTestExecution()
   {
      INSTANCE.executingTest.finishExecution();
   }

   // Methods to be called only from generated bytecode or from the MockingBridge /////////////////////////////////////

   @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
   public static boolean updateMockState(
      @NotNull String mockUpClassDesc, @Nullable Object mockedInstance, int mockStateIndex)
   {
      Object mockUp = getMock(mockUpClassDesc, mockedInstance);

      if (mockUp == null) {
         return false;
      }

      if (mockStateIndex < 0) {
         return true;
      }

      return getMockStates().updateMockState(mockUp, mockStateIndex);
   }

   @Nullable public static Object getMock(@NotNull String mockUpClassDesc, @Nullable Object mockedInstance)
   {
      return INSTANCE.mockClasses.getMock(mockUpClassDesc, mockedInstance);
   }
}
