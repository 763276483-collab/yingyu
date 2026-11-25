import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';

// Structural checks only. This script does not compile or execute Kotlin.
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const checks = [];
function check(name, ok) { checks.push({ name, passed: Boolean(ok) }); }
function read(file) { return fs.readFileSync(path.join(root, file), 'utf8'); }
const required = ['README.md','AGENTS.md','settings.gradle.kts','build.gradle.kts','app/build.gradle.kts',
  'gradlew','gradlew.bat','gradle/wrapper/gradle-wrapper.jar', 'gradle/wrapper/gradle-wrapper.properties',
  ...['REQUIREMENTS','ARCHITECTURE','DATA_MODEL','VOICE_RESEARCH','GLASS_DESIGN','API_DIAGNOSTICS','INSTALL','DECISIONS','DEBUGGING','VALIDATION','CHANGELOG','NEXT_STEPS'].map(x=>`docs/${x}.md`)];
for (const file of required) check(`exists: ${file}`, fs.existsSync(path.join(root, file)));
const jar = fs.readFileSync(path.join(root,'gradle/wrapper/gradle-wrapper.jar'));
check('wrapper JAR SHA-256 matches official value', crypto.createHash('sha256').update(jar).digest('hex') === '2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046');
check('Gradle distribution checksum pinned', read('gradle/wrapper/gradle-wrapper.properties').includes('distributionSha256Sum=f397b287023acdba1e9f6fc5ea72d22dd63669d59ed4a289a29b1a76eee151c6'));
const sample = JSON.parse(read('examples/app-data.example.json'));
check('example schema version', sample.schemaVersion === 2);
check('example glass settings in supported range', sample.appearance.glassOpacity >= 0.05 && sample.appearance.glassOpacity <= 0.48 && sample.appearance.blurRadiusDp >= 0 && sample.appearance.blurRadiusDp <= 32);
check('example has required character fields', sample.characters.every(x=>typeof x.id==='string' && x.name && x.persona && typeof x.description==='string' && typeof x.greeting==='string' && x.voice?.voiceId));
check('example defaults to cloud speech', sample.characters.every(x=>x.voice.provider==='CLOUD'));
check('example has no secret fields', !/"(?:apiKey|api_key|authorization|secret|accessToken)"\s*:/i.test(JSON.stringify(sample)));
check('example HTTPS endpoints', new URL(sample.api.baseUrl).protocol==='https:' && new URL(sample.ttsApi.endpoint).protocol==='https:');
check('Android disables backup', read('app/src/main/AndroidManifest.xml').includes('android:allowBackup="false"'));
check('Android rejects cleartext', read('app/src/main/AndroidManifest.xml').includes('android:usesCleartextTraffic="false"'));
const readmeLinks = [...read('README.md').matchAll(/\]\((docs\/[^)]+)\)/g)].map(x=>x[1]);
check('README local links resolve', readmeLinks.every(x=>fs.existsSync(path.join(root,x))));
console.log(JSON.stringify({scope:'structure only; not an Android build or Kotlin test run', checks: checks.length, passed: checks.filter(x=>x.passed).length, failed: checks.filter(x=>!x.passed)},null,2));
if (checks.some(x=>!x.passed)) process.exitCode=1;
