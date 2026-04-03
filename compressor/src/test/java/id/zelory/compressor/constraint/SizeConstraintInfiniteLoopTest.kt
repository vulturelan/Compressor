package id.zelory.compressor.constraint

import id.zelory.compressor.loadBitmap
import id.zelory.compressor.overWrite
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Test
import java.io.File

class SizeConstraintInfiniteLoopTest {

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test(expected = IllegalStateException::class, timeout = 30000)
    fun `reproduce IllegalStateException when quality is invalid`() {
        val tempFile = File.createTempFile("test_image", ".jpg")
        tempFile.writeBytes(ByteArray(2000))
        
        try {
            mockkStatic("id.zelory.compressor.UtilKt")
            every { loadBitmap(any<File>()) } returns mockk(relaxed = true)
            
            // Mock overWrite to simulate IllegalStateException when quality is out of bounds
            every { overWrite(any(), any(), any(), any()) } answers {
                val quality = it.invocation.args[3] as Int
                if (quality !in 0..100) {
                    throw IllegalStateException("quality must be 0..100")
                }
                tempFile
            }

            // Set minQuality to a negative value to allow quality to go out of bounds
            // if the safeguard is missing or if we don't handle negative results from calculation
            val constraint = SizeConstraint(
                maxFileSize = 1000, 
                stepSize = 10, 
                maxIteration = 100, 
                minQuality = -10 
            )

            var result = tempFile
            // This loop will eventually hit a negative quality if we don't stop it
            while (!constraint.isSatisfied(result)) {
                result = constraint.satisfy(result)
            }
            
        } finally {
            tempFile.delete()
        }
    }
}
