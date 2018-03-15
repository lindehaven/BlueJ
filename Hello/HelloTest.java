

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The test class HelloTest.
 *
 * @author  Lars Lindehaven
 * @version 2018-03-05
 */
public class HelloTest
{
    /**
     * Default constructor for test class HelloTest
     */
    public HelloTest()
    {
    }

    /**
     * Sets up the test fixture.
     *
     * Called before every test case method.
     */
    @Before
    public void setUp()
    {
    }

    /**
     * Tears down the test fixture.
     *
     * Called after every test case method.
     */
    @After
    public void tearDown()
    {
    }
    
    /**
     * Tests method go()
     */
    @Test
    public void testGo()
    {
        Hello hi = new Hello();
        hi.go();
    }

}
