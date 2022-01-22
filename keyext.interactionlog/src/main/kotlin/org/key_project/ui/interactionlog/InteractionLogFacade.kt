package org.key_project.ui.interactionlog

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.key_project.ui.interactionlog.model.InteractionLog
import java.io.File


/**
 * @author Alexander Weigl
 * @version 1 (06.12.18)
 */
object InteractionLogFacade {

    val mapper = ObjectMapper().apply {
        enable(SerializationFeature.INDENT_OUTPUT)
        findAndRegisterModules();
        configure(JsonParser.Feature.ALLOW_COMMENTS, true)
        configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
        //it.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
        //it.configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, true)
    }


    /**
     * @param inputFile
     * @return
     * @throws JAXBException
     */
    @JvmStatic
    fun readInteractionLog(inputFile: File): InteractionLog =
        mapper.readValue(inputFile, InteractionLog::class.java).also {
            it.savePath = inputFile
        }

    /**
     * @param log
     * @param output
     * @throws JAXBException
     */
    @JvmStatic
    fun storeInteractionLog(log: InteractionLog, output: File) {
        mapper.writeValue(output, log)
    }
}
