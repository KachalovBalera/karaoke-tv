const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const versionJsonPath = path.join(__dirname, 'version.json');
if (!fs.existsSync(versionJsonPath)) {
  console.error('Файл version.json не найден!');
  process.exit(1);
}

const versionJson = JSON.parse(fs.readFileSync(versionJsonPath, 'utf-8'));
const APP_VERSION = versionJson.version;
const RELEASE_TAG = `v${APP_VERSION}`;
const RELEASE_NAME = `Karaoke TV ${RELEASE_TAG}`;
const RELEASE_NOTES = versionJson.releaseNotes;

function getSecureToken() {
  if (process.env.GITHUB_TOKEN) return process.env.GITHUB_TOKEN.trim();
  const tokenFile = path.join(__dirname, '.github_token');
  if (fs.existsSync(tokenFile)) {
    return fs.readFileSync(tokenFile, 'utf-8').trim();
  }
  return null;
}

const GITHUB_TOKEN = getSecureToken();
const REPO = process.env.GITHUB_REPOSITORY || 'KachalovBalera/karaoke-tv';

const releaseApkPath = path.join(__dirname, 'app', 'build', 'outputs', 'apk', 'release', 'app-release.apk');
const assetFileName = `KaraokeTV_${RELEASE_TAG}.apk`;

async function main() {
  console.log(`=== Публикация обновления Karaoke TV ${RELEASE_TAG} на GitHub ===`);
  console.log(`Репозиторий: ${REPO}`);
  console.log(`Версия: ${APP_VERSION}`);

  const projectDir = __dirname;
  if (!fs.existsSync(releaseApkPath) && !process.env.SKIP_BUILD) {
    console.log('\nЗапускаем сборку release APK (assembleRelease)...');
    try {
      execSync(`cmd /c "subst K: /d & subst K: \\"${projectDir}\\" && cd /d K:\\ && gradlew.bat assembleRelease && subst K: /d"`, {
        stdio: 'inherit'
      });
    } catch {
      execSync('gradlew.bat assembleRelease --console=plain', {
        cwd: projectDir,
        stdio: 'inherit'
      });
    }
  }

  if (!fs.existsSync(releaseApkPath)) {
    console.error(`Ошибка: APK файл не найден по пути ${releaseApkPath}`);
    process.exit(1);
  }

  const apkStats = fs.statSync(releaseApkPath);
  const sizeMb = (apkStats.size / (1024 * 1024)).toFixed(1);
  console.log(`\nНайден APK: ${releaseApkPath} (${sizeMb} MB)`);

  if (!GITHUB_TOKEN) {
    console.log('\n[Внимание] GITHUB_TOKEN не задан.');
    console.log('Для автоматической загрузки на GitHub Releases укажите переменную GITHUB_TOKEN или создайте файл .github_token.');
    console.log(`\nВы можете вручную создать релиз на https://github.com/${REPO}/releases/new`);
    console.log(`- Tag: ${RELEASE_TAG}`);
    console.log(`- Title: ${RELEASE_NAME}`);
    console.log(`- Файл: ${releaseApkPath} (переименовать в ${assetFileName})`);
    return;
  }

  const headers = {
    Authorization: `token ${GITHUB_TOKEN}`,
    'User-Agent': 'KaraokeTV-Release-Bot',
    Accept: 'application/vnd.github.v3+json',
  };

  console.log(`\nСоздание релиза ${RELEASE_TAG} на GitHub...`);
  const createRes = await fetch(`https://api.github.com/repos/${REPO}/releases`, {
    method: 'POST',
    headers: { ...headers, 'Content-Type': 'application/json' },
    body: JSON.stringify({
      tag_name: RELEASE_TAG,
      target_commitish: 'main',
      name: RELEASE_NAME,
      body: RELEASE_NOTES,
      draft: false,
      prerelease: false,
    }),
  });

  let releaseData;
  if (createRes.status === 201) {
    releaseData = await createRes.json();
    console.log(`Релиз успешно создан! ID: ${releaseData.id}`);
  } else if (createRes.status === 422) {
    console.log(`Релиз ${RELEASE_TAG} уже существует, получаем его данные...`);
    const getRes = await fetch(`https://api.github.com/repos/${REPO}/releases/tags/${RELEASE_TAG}`, {
      headers,
    });
    releaseData = await getRes.json();
  } else {
    const errorText = await createRes.text();
    console.error(`Ошибка при создании релиза: ${createRes.status} ${errorText}`);
    process.exit(1);
  }

  // Если ассет уже есть в релизе, удаляем старый
  if (releaseData.assets && releaseData.assets.length > 0) {
    for (const asset of releaseData.assets) {
      if (asset.name === assetFileName) {
        console.log(`Удаляем предыдущий файл ${assetFileName} (ID: ${asset.id})...`);
        await fetch(`https://api.github.com/repos/${REPO}/releases/assets/${asset.id}`, {
          method: 'DELETE',
          headers,
        });
      }
    }
  }

  // Загрузка APK в релиз
  console.log(`\nЗагрузка файла ${assetFileName} (${sizeMb} MB) в релиз...`);
  const uploadUrl = releaseData.upload_url.replace('{?name,label}', `?name=${encodeURIComponent(assetFileName)}`);
  const apkBuffer = fs.readFileSync(releaseApkPath);

  const uploadRes = await fetch(uploadUrl, {
    method: 'POST',
    headers: {
      Authorization: `token ${GITHUB_TOKEN}`,
      'User-Agent': 'KaraokeTV-Release-Bot',
      'Content-Type': 'application/vnd.android.package-archive',
      'Content-Length': apkBuffer.length,
    },
    body: apkBuffer,
  });

  if (uploadRes.status === 201) {
    const assetData = await uploadRes.json();
    console.log(`APK успешно загружен! Прямая ссылка: ${assetData.browser_download_url}`);

    // Обновляем apkUrl в version.json
    versionJson.apkUrl = assetData.browser_download_url;
    fs.writeFileSync(versionJsonPath, JSON.stringify(versionJson, null, 2), 'utf-8');
    console.log('Файл version.json обновлен актуальной ссылкой на релиз!');

    // Автоматически обновляем version.json в репозитории GitHub на ветке main
    try {
      const getFileRes = await fetch(`https://api.github.com/repos/${REPO}/contents/version.json`, { headers });
      if (getFileRes.ok) {
        const fileData = await getFileRes.json();
        const updatedContent = Buffer.from(fs.readFileSync(versionJsonPath, 'utf-8')).toString('base64');
        const updateRes = await fetch(`https://api.github.com/repos/${REPO}/contents/version.json`, {
          method: 'PUT',
          headers: { ...headers, 'Content-Type': 'application/json' },
          body: JSON.stringify({
            message: `Update version.json to ${RELEASE_TAG}`,
            content: updatedContent,
            sha: fileData.sha,
            branch: 'main'
          })
        });
        if (updateRes.ok) {
          console.log(`Файл version.json в репозитории GitHub (main) успешно обновлен до ${RELEASE_TAG}!`);
        }
      }
    } catch (e) {
      console.warn('Не удалось обновить version.json в репозитории GitHub:', e.message);
    }
  } else {
    const errText = await uploadRes.text();
    console.error(`Ошибка загрузки APK: ${uploadRes.status} ${errText}`);
  }

  console.log('\n=== Готово! ===');
}

main().catch(err => {
  console.error('Непредвиденная ошибка:', err);
  process.exit(1);
});
