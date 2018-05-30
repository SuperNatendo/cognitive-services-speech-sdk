//
// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE.md file in the project root for full license information.
//
//

#if defined(_MSC_VER) || defined(_CODECVT_H)
#include <codecvt>
#else
#include <strings.h>
#include <cstdlib>
#include <clocale>
#endif

#include <wchar.h>
#include <assert.h>
#include <algorithm>
#include <vector>
#include "string_utils.h"

namespace PAL {

int stricmp(const char *a, const char *b)
{
#ifdef _MSC_VER
    return _stricmp(a, b);
#else
    return ::strcasecmp(a, b);
#endif
}

int wcsicmp(const wchar_t *a, const wchar_t *b)
{
#ifdef _MSC_VER
    return _wcsicmp(a, b);
#else
    return ::wcscasecmp(a, b);
#endif
}

int wcsnicmp(const wchar_t *a, const wchar_t *b, size_t n)
{
#ifdef _MSC_VER
        return _wcsnicmp(a, b, n);
#else
        return ::wcsncasecmp(a, b, n);
#endif
}

// Similarly to strcpy_s, wcscpy_s functions, dstSize and srcSize are sizes of corresponding
// arrays in terms of number of elements (in wide characters).
void wcscpy(wchar_t *dst, size_t dstSize, const wchar_t *src, size_t srcSize, bool truncate)
{
    // TODO (alrezni): throw instead of asserting, 
    // see https://msdn.microsoft.com/en-us/library/5dae5d43.aspx
    // and https://msdn.microsoft.com/en-us/library/td1esda9.aspx
    // for more details on error conditions, add unit tests.
    assert(src);
    assert(dst);
    assert(dstSize != 0);

    auto toCopy = std::min(dstSize, srcSize);

    if (!truncate)
    {
        assert(dstSize > toCopy);
    }

#ifdef _MSC_VER
    wcsncpy_s(dst, dstSize, src, toCopy);
#else
    wcsncpy(dst, src, toCopy);
#endif

    dst[std::min(toCopy, dstSize-1)] = 0; // make sure the string is null-terminated
}


std::string ToString(const std::wstring& wstring)
{
#ifdef _MSC_VER
    std::wstring_convert<std::codecvt_utf8<wchar_t>, wchar_t> converter;
    return converter.to_bytes(wstring);
#else
    const auto length = wstring.length() * sizeof(std::wstring::value_type) + 1;
    std::vector<char> buf(length);
    const auto res = std::wcstombs(buf.data(), wstring.c_str(), sizeof(char)*length);
    return (res <= length) ? buf.data() : "";
#endif
}

std::wstring ToWString(const std::string& string)
{
#ifdef _MSC_VER
    std::wstring_convert<std::codecvt_utf8<wchar_t>, wchar_t> converter;
    return converter.from_bytes(string);
#else
    const auto length = string.length() + 1;
    std::vector<wchar_t> buf(length);
    const auto res = std::mbstowcs(buf.data(), string.c_str(), sizeof(wchar_t)*length);
    return (res <= length) ? buf.data() : L"";
#endif
}

} // PAL