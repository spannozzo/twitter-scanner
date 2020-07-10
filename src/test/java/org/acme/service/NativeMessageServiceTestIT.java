package org.acme.service;

import io.quarkus.test.junit.NativeImageTest;

@NativeImageTest
public class NativeMessageServiceTestIT extends MessageServiceTest {

    // Execute the same tests but in native mode.
}