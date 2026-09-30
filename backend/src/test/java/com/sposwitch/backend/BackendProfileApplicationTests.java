package com.sposwitch.backend;

import org.springframework.test.context.ActiveProfiles;

/** Both existing profile files must remain usable without database environment variables. */
@ActiveProfiles({"local", "test"})
class BackendProfileApplicationTests extends BackendApplicationTests {
}
