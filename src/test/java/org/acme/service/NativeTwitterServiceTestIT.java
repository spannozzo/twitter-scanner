package org.acme.service;

import io.quarkus.test.junit.NativeImageTest;

@NativeImageTest
public class NativeTwitterServiceTestIT extends TwitterServiceTest {

    // Execute the same tests but in native mode.
}