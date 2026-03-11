const { readFileSync, existsSync, mkdirSync } = require('fs');
const { parse, resolve } = require('path');
const AdmZip = require('adm-zip');

try {
  const { version } = JSON.parse(
    readFileSync(resolve(__dirname, 'dist/chrome', 'manifest.json'), 'utf8')
  );

  const { name } = JSON.parse(
    readFileSync(resolve(__dirname,'package.json'), 'utf8')
  );

  if (!name) {
    throw new Error('Missing "name" field in package.json');
  }
  const safeName = name.replace(/^@/, '').replace(/[\/\\]/g, '-');

  const outdir = 'release';
  const supportedBrowser = ['chrome', 'firefox']
  supportedBrowser.forEach(browser => {
    const filename = `${safeName}-${browser}-v${version}.zip`;
    const zip = new AdmZip();
    zip.addLocalFolder(`dist/${browser}`);
    if (!existsSync(outdir)) {
      mkdirSync(outdir);
    }
    zip.writeZip(`${outdir}/${filename}`);

    console.log(
      `Success! Created a ${filename} file under ${outdir} directory.`
    );

  })

} catch (e) {
  console.error('Error! Failed to generate a zip file.' + e);
}
