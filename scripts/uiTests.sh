#!/bin/bash
set -ex
source config.init

# Upload app and testing apk
echo "Uploading app APK to Browserstack..."
upload_app_response="$(curl -u $browserstack_username:$browserstack_access_key -X POST https://api-cloud.browserstack.com/app-automate/upload -F file=@$app_apk_path)"
app_url=$(echo "$upload_app_response" | jq .app_url)

echo "Uploading test APK to Browserstack..."
upload_test_response="$(curl -u $browserstack_username:$browserstack_access_key -X POST https://api-cloud.browserstack.com/app-automate/espresso/test-suite -F file=@$test_apk_path)"
test_url=$(echo "$upload_test_response" | jq .test_url)

#Check total builds running
#Check builds running in primary
number_of_active_builds_in_primary_response="$(curl -X GET "https://api.bitrise.io/v0.1/apps/c9d408d1d812ee38/builds?workflow=primary&status=0" -H  "accept: application/json")"
number_of_active_builds=$(echo "$number_of_active_builds_in_primary_response" | jq -r .paging.total_item_count)

#Check builds running in UITestingSpecific
number_of_active_builds_in_ui_specific_response="$(curl -X GET "https://api.bitrise.io/v0.1/apps/c9d408d1d812ee38/builds?workflow=UITestingSpecific&status=0" -H  "accept: application/json")"
number_of_active_builds+=$(echo "$number_of_active_builds_in_ui_specific_response" | jq -r .paging.total_item_count)

#If this is the third build, we use only one device to launch the tests
if [[ $number_of_active_builds = $bitrise_max_parallel_builds ]];
then
    browserstack_number_of_parallel_executions=1
fi

# Prepare json and run tests
echo "Starting execution of espresso tests..."
shards=$(jq -n \
                --arg number_of_shards "$browserstack_number_of_parallel_executions" \
                '{numberOfShards: $number_of_shards}')

json=$(jq -n \
                --argjson app_url $app_url \
                --argjson test_url $test_url \
                --argjson devices ["$browserstack_device_list"] \
                --argjson package ["$browserstack_package"] \
                --argjson class ["$browserstack_class"] \
                --argjson annotation ["$browserstack_annotation"] \
                --argjson size ["$browserstack_size"] \
                --arg logs "$browserstack_device_logs" \
                --arg video "$browserstack_video" \
                --arg loc "$browserstack_local" \
                --arg locId "$browserstack_local_identifier" \
                --arg gpsLocation "$browserstack_gps_location" \
                --arg language "$browserstack_language" \
                --arg locale "$browserstack_locale" \
                --arg deviceLogs "$browserstack_deviceLogs" \
                --argjson shards "$shards" \
                '{devices: $devices, app: $app_url, testSuite: $test_url, package: $package, class: $class, annotation: $annotation, size: $size, logs: $logs, video: $video, local: $loc, localIdentifier: $locId, gpsLocation: $gpsLocation, language: $language, locale: $locale, deviceLogs: $deviceLogs, shards: $shards}')

test_execution_response="$(curl -X POST https://api-cloud.browserstack.com/app-automate/espresso/v2/build -d \ "$json" -H "Content-Type: application/json" -u "$browserstack_username:$browserstack_access_key")"

# Get build
build_id=$(echo "$test_execution_response" | jq -r .build_id)
echo "build id running: $build_id"

# Monitor build status
echo "Monitoring build status started...."

# Export test reports to bitrise
test_reports_url="https://app-automate.browserstack.com/dashboard/v2/builds/$build_id"
echo $test_reports_url
envman add --key BROWSERSTACK_TEST_REPORTS --value "$test_reports_url"

# weird behavior from Browserstack api, you can have "done" status with failed tests
# "devices" only show one device result which is inconsistance
# then "device_status" is checked
exit 0
