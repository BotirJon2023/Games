const crawler: Crawler = async ({ page, url, platformKey, sourceId, env, extensions
}) => {
        const { platformSpecificId: existingPlatformSpecificId } = extensions ?? {};
        const response = await safeGoto(page, url, 'domcontentloaded');
    const { normalizedUrl, platformSpecificId } = getNormalizedUrl({ url: response.url(), platformKey });

        if (normalizedUrl === undefined) {
        throw new InvalidUrlError('Redirect Url is not a valid user profile');
    }

await syncSource({
    platformSpecificId,
    existingPlatformSpecificId,
    sourceId,
    normalizedUrl,
    existingUrl: url,
    env,
});

        const notice = await page.$('div.page-notice__main');

    if (!isNil(notice)) {
        const noticeText = await notice.evaluate((element) => element.textContent);

        if (noticeText?.startsWith('Dieses Profil ist nicht verfügbar')) {
        throw new FoundNoUserProfileError('This profile is unavailable');
        }
    }

let ratingValue;
let valueDsr1 = 0;
let valueDsr2 = 0;
let valueDsr3 = 0;
let valueDsr4 = 0;
let countDsr1 = 0;
let countDsr2 = 0;
let countDsr3 = 0;
let countDsr4 = 0;

// DSR Rating extraction blocks (collapsed in IDE)
    try {
       const results = await getRating1(page);

       valueDsr1 = results.valueDsr1;
       countDsr1 = results.countDsr1;
    } catch {
    }
            try {
            const results = await getRating2(page);

valueDsr2 = results.valueDsr2;
countDsr2 = results.countDsr2;
  } catch {
          // keep default values
          }

          try {
          const results = await getRating3(page);

valueDsr3 = results.valueDsr3;
countDsr3 = results.countDsr3;
  } catch {
          // keep default values
          }

          try {
          const results = await getRating4(page);

valueDsr4 = results.valueDsr4;
countDsr4 = results.countDsr4;
  } catch {
          // keep default values
          }


            const countArray = [countDsr1, countDsr2, countDsr3, countDsr4];
        const validCategories = countArray.filter((element) => element !== 0);

        if (validCategories.length > 0) {
ratingValue = (valueDsr1 * countDsr1 + valueDsr2 * countDsr2 +
valueDsr3 * countDsr3 + valueDsr4 * countDsr4) /
        (countDsr1 + countDsr2 + countDsr3 + countDsr4);
        } else {
        // Fallback with syntax highlighting:
        const posPercentageText = await extractValue({
    page,
            type: 'value',
            selector: 'span[data-testid="user-positive-fdbk"]',
            pageFunction: (elements) => elements.map((element) => element.textContent),
});

        const percentageMatch = /(\d+(?:\.\d*)?)%/u.exec(posPercentageText);
        const percentage = percentageMatch ? Number.parseFloat(percentageMatch[1]) : 0;

ratingValue = Math.max(1, Math.min(5,
                       Number.parseFloat(((percentage / 100) * 5).toFixed(1))
        );
        }

        const ratingCountText = await extractValue({
    page,
            type: 'count',
            selector: 'span[data-testid="details-total-fdbk"]',
            pageFunction: (elements) => elements.map((element) => element.textContent),
});

let ratingCount = '0';
    const countMatch = /^([\d,]+)/u.exec(ratingCountText);
    if (countMatch) {
ratingCount = countMatch[1].replace(/,/gu, '');
    }

            return {
ratingValue: Math.max(1, ratingValue),
ratingCount,
extensions: {
normalizedUrl,
platformSpecificId,
        },
        };
        };

        const ebay: CrawlerConfig = {
crawler,
        };

export { ebay, debug };