package asset.pipeline

import asset.pipeline.AssetPipelineConfigHolder

class AssetPipelineBootStrap {

    def grailsApplication

    def init = { servletContext ->
        def storagePath = grailsApplication.config.grails.assets.storagePath
        if (!storagePath) {
            return
        }
        def manifest = AssetPipelineConfigHolder.manifest
        log.debug("asset pipeline bootstrap manifest exists: ${manifest != null}")

        if(manifest) {
            def storageFile = new File(storagePath)
            storageFile.mkdirs()
            manifest.stringPropertyNames().each { propertyName ->
                log.debug("processing property: ${propertyName}")
                def propertyValue = manifest.getProperty(propertyName)
                def res = grailsApplication.getParentContext().getResource("assets/${propertyValue}")
                if(!res.exists()) {
                    res = grailsApplication.getParentContext().getResource("classpath:assets/${propertyValue}")
                }
                def outputFile = new File(storagePath, propertyName)
                def parentFile = new File(outputFile.parent)
                parentFile.mkdirs()
                def outputDigestFile = new File(storagePath, propertyValue)
                copyFile(res.inputStream,outputFile,outputDigestFile)
                def gzRes = grailsApplication.getParentContext().getResource("assets/${propertyValue}.gz")
                if(!gzRes.exists()) {
                    gzRes = grailsApplication.getParentContext().getResource("classpath:assets/${propertyValue}.gz")
                }
                if(gzRes.exists()) {
                    def outputGzFile = new File(storagePath, "${propertyName}.gz")
                    def outputGzDigestFile = new File(storagePath, "${propertyValue}.gz")
                    copyFile(gzRes.inputStream,outputGzFile,outputGzDigestFile)
                }
            }
            def manifestFile = new File(storagePath,'manifest.properties')
            manifest.store(manifestFile.newWriter(),"")
            log.debug("storing manifest file at: " + manifestFile.absolutePath)
        }
    }

    def copyFile(sourceStream, targetFile, digestFile) {
        try {
            if(!targetFile.exists()) {
                targetFile.createNewFile()
            }
            if(!digestFile?.exists()) {
                digestFile?.createNewFile()
            }
            def outputFileStream = targetFile.newOutputStream()
            def outputDigestFileStream = digestFile?.newOutputStream()
            byte[] buffer = new byte[8192]
            int nRead
            while((nRead = sourceStream.read(buffer, 0, buffer.length)) != -1) {
                // noop (just to complete the stream)
                outputFileStream?.write(buffer, 0, nRead);
                outputDigestFileStream?.write(buffer, 0, nRead);
            }
            outputFileStream.flush()
            outputFileStream.close()
            outputDigestFileStream.flush()
            outputDigestFileStream.close()
            try {
                targetFile.setReadable(true,false)
                targetFile.setExecutable(true,false)
                targetFile.setWritable(true)
                digestFile?.setReadable(true,false)
                digestFile?.setExecutable(true,false)
                digestFile?.setWritable(true)
            } catch (ex) {
               // attempting permission set
            }
        } finally {
            sourceStream.close()
        }
        
    }
}
