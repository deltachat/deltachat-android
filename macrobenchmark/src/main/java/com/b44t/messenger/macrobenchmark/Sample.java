package com.b44t.messenger.macrobenchmark;

import android.content.Intent;

import androidx.benchmark.junit4.BenchmarkRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
class SampleStartupBenchmark {
  @Rule
  BenchmarkRule benchmarkRule = MacrobenchmarkRule();

  @Test
  void startup() = benchmarkRule.measureRepeated(
  "mypackage.myapp",
  listOf(StartupTimingMetric()),
  5,
  StartupMode.COLD
    ) { // this = MacrobenchmarkScope
    pressHome();
    Intent intent = Intent();
    intent.setPackage("mypackage.myapp");
    intent.setAction("mypackage.myapp.myaction");
    startActivityAndWait(intent);
  }
}
