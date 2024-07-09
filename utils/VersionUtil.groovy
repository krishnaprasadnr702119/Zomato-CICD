package org.mycompany.utils

class VersionUtil {

    String getVersion(String env) {
        def currentDate = new Date().format('yyyyMMddHHmmss')
        def versionPrefix = getVersionPrefix(env)
        return "${versionPrefix}-${currentDate}"
    }

    private String getVersionPrefix(String env) {
        switch(env) {
            case 'dev':
                return "dev"
            case 'staging':
                return "stg"
            case 'uat':
                return "uat"
            case 'production':
                return "prod"
            default:
                throw new IllegalArgumentException("Unknown environment: ${env}")
        }
    }
}
