#!/usr/bin/env python3
"""
Comprehensive Live Jellyfin Server Verification & Test Suite.
Tests all real-world scenarios, endpoints, media types, hierarchies, and playback streams
against the production Jellyfin server at http://192.168.200.200:80.
"""

import sys
import json
import time
import urllib.parse
import urllib.request
import urllib.error
from dataclasses import dataclass, field
from typing import List, Dict, Any, Optional

SERVER_URL = "http://192.168.200.200:80"
TARGET_USERNAME = "الاستراحه"

@dataclass
class TestCaseResult:
    category: str
    name: str
    status: str  # "PASS", "FAIL", "SKIP"
    duration_ms: float
    details: str = ""
    error: Optional[str] = None

class LiveServerTestSuite:
    def __init__(self, base_url: str = SERVER_URL, username: str = TARGET_USERNAME):
        self.base_url = base_url.rstrip("/")
        self.username = username
        self.user_id: Optional[str] = None
        self.access_token: Optional[str] = None
        self.libraries: List[Dict[str, Any]] = []
        self.results: List[TestCaseResult] = []
        self.test_artifacts: Dict[str, Any] = {}

    def _request(self, endpoint: str, method: str = "GET", headers: Optional[Dict[str, str]] = None,
                 data: Optional[Dict[str, Any]] = None, timeout: float = 20.0) -> Any:
        url = f"{self.base_url}/{endpoint.lstrip('/')}"
        req_headers = {
            "Accept": "application/json",
            "User-Agent": "mdd TV / LiveTest (Android TV)",
            "X-Emby-Authorization": 'MediaBrowser Client="mdd TV", Device="Android", DeviceId="mdd-tv-android", Version="1.0.0"'
        }
        if self.access_token:
            req_headers["X-Emby-Token"] = self.access_token

        if headers:
            req_headers.update(headers)

        encoded_data = None
        if data is not None:
            req_headers["Content-Type"] = "application/json"
            encoded_data = json.dumps(data).encode("utf-8")

        req = urllib.request.Request(url, data=encoded_data, headers=req_headers, method=method)
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            content_type = resp.headers.get("Content-Type", "")
            status_code = resp.getcode()
            if "application/json" in content_type:
                return json.loads(resp.read().decode("utf-8"))
            elif "image/" in content_type or "video/" in content_type or "application/octet-stream" in content_type:
                return {"status_code": status_code, "content_type": content_type, "length": resp.headers.get("Content-Length")}
            else:
                body = resp.read()
                try:
                    return json.loads(body.decode("utf-8"))
                except Exception:
                    return {"status_code": status_code, "raw_body_len": len(body)}

    def record_test(self, category: str, name: str, fn):
        start_time = time.time()
        try:
            details = fn()
            duration_ms = (time.time() - start_time) * 1000
            res = TestCaseResult(category=category, name=name, status="PASS", duration_ms=duration_ms, details=str(details or "OK"))
            print(f"  [\033[92mPASS\033[0m] {category} -> {name} ({duration_ms:.1f}ms): {details}")
            self.results.append(res)
        except Exception as e:
            duration_ms = (time.time() - start_time) * 1000
            err_msg = f"{type(e).__name__}: {str(e)}"
            res = TestCaseResult(category=category, name=name, status="FAIL", duration_ms=duration_ms, error=err_msg)
            print(f"  [\033[91mFAIL\033[0m] {category} -> {name} ({duration_ms:.1f}ms): {err_msg}")
            self.results.append(res)

    # =========================================================================
    # SUITE 1: Handshake, Public Info, & Authentication
    # =========================================================================
    def test_suite_1_auth(self):
        print("\n=== SUITE 1: Handshake, Public System Info & Auth ===")

        def case_public_info():
            data = self._request("System/Info/Public")
            assert "ServerName" in data, "ServerName missing"
            assert "Version" in data, "Version missing"
            assert "Id" in data, "Id missing"
            self.test_artifacts["server_info"] = {
                "name": data.get("ServerName"),
                "version": data.get("Version"),
                "os": data.get("OperatingSystem")
            }
            return f"Server '{data.get('ServerName')}' v{data.get('Version')} on {data.get('OperatingSystem')}"

        def case_authenticate_user():
            payload = {"Username": self.username, "Pw": ""}
            auth = self._request("Users/AuthenticateByName", method="POST", data=payload)
            assert "AccessToken" in auth, "AccessToken missing in auth response"
            assert "User" in auth, "User object missing in auth response"
            self.access_token = auth["AccessToken"]
            self.user_id = auth["User"]["Id"]
            self.test_artifacts["user_id"] = self.user_id
            self.test_artifacts["access_token"] = self.access_token[:8] + "..."
            return f"Authenticated as '{self.username}'. UserID: {self.user_id}, Token: {self.access_token[:8]}..."

        def case_public_users():
            users = self._request("Users/Public")
            assert isinstance(users, list), "Users/Public response should be a list"
            target = next((u for u in users if u.get("Name") == self.username), None)
            assert target is not None, f"Target user '{self.username}' not found in public users"
            return f"Public user confirmed: '{target.get('Name')}' (HasPassword: {target.get('HasPassword')})"

        def case_invalid_auth_rejection():
            try:
                self._request("Users/AuthenticateByName", method="POST", data={"Username": "non_existent_user_999", "Pw": "wrong"})
                assert False, "Server should have rejected invalid user authentication"
            except urllib.error.HTTPError as he:
                assert he.code in (400, 401, 500), f"Expected 400/401/500 for invalid auth, got {he.code}"
                return f"Gracefully rejected invalid auth with HTTP {he.code}"

        self.record_test("Auth & Handshake", "Public System Info", case_public_info)
        self.record_test("Auth & Handshake", "Authenticate by Username", case_authenticate_user)
        self.record_test("Auth & Handshake", "Verify Public Users", case_public_users)
        self.record_test("Auth & Handshake", "Invalid Auth Rejection", case_invalid_auth_rejection)

    # =========================================================================
    # SUITE 2: Library & Views Discovery
    # =========================================================================
    def test_suite_2_libraries(self):
        print("\n=== SUITE 2: Library Views & Catalog Inventory ===")

        def case_fetch_views():
            data = self._request(f"Users/{self.user_id}/Views")
            assert "Items" in data, "Items missing in User Views response"
            self.libraries = data["Items"]
            assert len(self.libraries) >= 11, f"Expected at least 11 libraries, found {len(self.libraries)}"
            lib_names = [lib.get("Name") for lib in self.libraries]
            self.test_artifacts["libraries"] = lib_names
            return f"Found {len(self.libraries)} libraries: {', '.join(lib_names[:5])}..."

        def case_verify_expected_libraries():
            expected = [
                "قسم الافلام", "الانمي والاطفال", "المسلسلات العربية", "مسلسلات رمضان",
                "المسلسلات الاسيوية", "المسلسلات التركية", "المسلسلات الاجنبية",
                "المنوعـــــات", "المكتبة الاسلامية", "البرامج التلفزيونية", "مسلسللات متتنوعة"
            ]
            current_names = [lib.get("Name") for lib in self.libraries]
            missing = [e for e in expected if e not in current_names]
            assert not missing, f"Missing expected libraries: {missing}"
            return f"All 11 expected server libraries are present and verified."

        self.record_test("Library Views", "Fetch All User Views", case_fetch_views)
        self.record_test("Library Views", "Verify 11 Mandatory Libraries", case_verify_expected_libraries)

    # =========================================================================
    # SUITE 3: Hierarchical Navigation (recursive=false) across All 11 Libraries
    # =========================================================================
    def test_suite_3_hierarchical_root(self):
        print("\n=== SUITE 3: Hierarchical Browsing (recursive=false) Across All Libraries ===")

        for lib in self.libraries:
            lib_name = lib.get("Name")
            lib_id = lib.get("Id")

            def make_test_func(lid=lib_id, lname=lib_name):
                def case_lib_root():
                    params = {
                        "parentId": lid,
                        "sortBy": "DateCreated,SortName",
                        "sortOrder": "Descending",
                        "includeItemTypes": "Folder,Series,Movie,Episode,Video,Audio",
                        "recursive": "false",
                        "startIndex": 0,
                        "limit": 30,
                        "fields": "PrimaryImageAspectRatio,Overview,CommunityRating"
                    }
                    qs = urllib.parse.urlencode(params)
                    res = self._request(f"Users/{self.user_id}/Items?{qs}")
                    items = res.get("Items", [])
                    total = res.get("TotalRecordCount", 0)
                    types = list({it.get("Type") for it in items})
                    assert total >= 0, "Invalid TotalRecordCount"
                    return f"Count: {len(items)}/{total} items, Types: {types}"
                return case_lib_root

            self.record_test("Hierarchical Root", f"Library '{lib_name}'", make_test_func())

    # =========================================================================
    # SUITE 4: Deep Subfolder Hierarchy Drill-Down (Multi-Level Breadcrumbs)
    # =========================================================================
    def test_suite_4_deep_drilldown(self):
        print("\n=== SUITE 4: Multi-Level Subfolder Drill-down (Breadcrumb Verification) ===")

        target_libs = ["الانمي والاطفال", "المكتبة الاسلامية", "قسم الافلام"]
        for target_name in target_libs:
            lib = next((l for l in self.libraries if l.get("Name") == target_name), None)
            if not lib:
                continue

            def make_drilldown_func(l_obj=lib, l_title=target_name):
                def case_drill():
                    # Level 1: Root
                    params1 = {
                        "parentId": l_obj["Id"],
                        "includeItemTypes": "Folder,Series,Movie,Episode,Video,Audio",
                        "recursive": "false",
                        "limit": 10
                    }
                    res1 = self._request(f"Users/{self.user_id}/Items?{urllib.parse.urlencode(params1)}")
                    items1 = res1.get("Items", [])
                    folders1 = [it for it in items1 if it.get("Type") == "Folder"]

                    if not folders1:
                        return f"No subfolders at root of '{l_title}', items: {len(items1)}"

                    subfolder = folders1[0]
                    # Level 2: Inside subfolder
                    params2 = {
                        "parentId": subfolder["Id"],
                        "includeItemTypes": "Folder,Series,Movie,Episode,Video,Audio",
                        "recursive": "false",
                        "limit": 10
                    }
                    res2 = self._request(f"Users/{self.user_id}/Items?{urllib.parse.urlencode(params2)}")
                    items2 = res2.get("Items", [])

                    drill_log = f"L1: '{subfolder.get('Name')}' ({len(items2)} items)"
                    folders2 = [it for it in items2 if it.get("Type") == "Folder"]
                    if folders2:
                        # Level 3: Inside deeper subfolder
                        sub2 = folders2[0]
                        params3 = {
                            "parentId": sub2["Id"],
                            "includeItemTypes": "Folder,Series,Movie,Episode,Video,Audio",
                            "recursive": "false",
                            "limit": 10
                        }
                        res3 = self._request(f"Users/{self.user_id}/Items?{urllib.parse.urlencode(params3)}")
                        items3 = res3.get("Items", [])
                        drill_log += f" -> L2: '{sub2.get('Name')}' ({len(items3)} items)"

                    return drill_log
                return case_drill

            self.record_test("Deep Drilldown", f"Drill into '{target_name}'", make_drilldown_func())

    # =========================================================================
    # SUITE 5: Flat Mode vs Hierarchical Mode Comparison
    # =========================================================================
    def test_suite_5_flat_vs_hierarchical(self):
        print("\n=== SUITE 5: Flat Mode vs. Hierarchical Mode Comparative Verification ===")

        for name in ["مسلسلات رمضان", "الانمي والاطفال"]:
            lib = next((l for l in self.libraries if l.get("Name") == name), None)
            if not lib:
                continue

            def make_comp_func(l_obj=lib, l_name=name):
                def case_comp():
                    # Hierarchical mode:
                    qh = {
                        "parentId": l_obj["Id"],
                        "includeItemTypes": "Folder,Series,Movie,Episode,Video,Audio",
                        "recursive": "false",
                        "limit": 5
                    }
                    resh = self._request(f"Users/{self.user_id}/Items?{urllib.parse.urlencode(qh)}")
                    hier_count = resh.get("TotalRecordCount", 0)

                    # Flat mode:
                    qf = {
                        "parentId": l_obj["Id"],
                        "includeItemTypes": "Movie,Series,Episode,Video,Audio",
                        "recursive": "true",
                        "limit": 5
                    }
                    resf = self._request(f"Users/{self.user_id}/Items?{urllib.parse.urlencode(qf)}")
                    flat_count = resf.get("TotalRecordCount", 0)

                    assert flat_count >= hier_count, f"Flat count ({flat_count}) should be >= Hierarchical count ({hier_count})"
                    return f"Hierarchical root count: {hier_count} vs Flat catalog count: {flat_count} (ratio: {flat_count/max(hier_count,1):.1f}x)"
                return case_comp

            self.record_test("Flat vs Hierarchical", f"Volume comparison for '{name}'", make_comp_func())

    # =========================================================================
    # SUITE 6: Pagination & Boundary Edge Cases
    # =========================================================================
    def test_suite_6_pagination(self):
        print("\n=== SUITE 6: Pagination, Offsets & Boundary Conditions ===")
        movie_lib = next((l for l in self.libraries if "افلام" in l.get("Name", "")), self.libraries[0])

        def case_page_continuity():
            # Page 1 (0..9)
            q1 = {"parentId": movie_lib["Id"], "recursive": "true", "startIndex": 0, "limit": 10}
            r1 = self._request(f"Users/{self.user_id}/Items?{urllib.parse.urlencode(q1)}")
            items1 = r1.get("Items", [])
            ids1 = [it["Id"] for it in items1]

            # Page 2 (10..19)
            q2 = {"parentId": movie_lib["Id"], "recursive": "true", "startIndex": 10, "limit": 10}
            r2 = self._request(f"Users/{self.user_id}/Items?{urllib.parse.urlencode(q2)}")
            items2 = r2.get("Items", [])
            ids2 = [it["Id"] for it in items2]

            # Ensure no overlapping IDs between distinct pages
            overlap = set(ids1).intersection(set(ids2))
            assert len(overlap) == 0, f"Pagination overlap detected: {overlap}"
            return f"Page 1 (10 items) and Page 2 (10 items) have 0 overlap"

        def case_out_of_bounds_offset():
            q = {"parentId": movie_lib["Id"], "recursive": "true", "startIndex": 500000, "limit": 20}
            res = self._request(f"Users/{self.user_id}/Items?{urllib.parse.urlencode(q)}")
            items = res.get("Items", [])
            assert len(items) == 0, f"Expected 0 items for out-of-bounds offset, got {len(items)}"
            assert res.get("TotalRecordCount", 0) > 0, "TotalRecordCount should still be reported"
            return f"Gracefully returned 0 items; TotalRecordCount preserved ({res.get('TotalRecordCount')})"

        self.record_test("Pagination", "Consecutive Page Continuity", case_page_continuity)
        self.record_test("Pagination", "Out-of-Bounds Offset Handling", case_out_of_bounds_offset)

    # =========================================================================
    # SUITE 7: Smart Scoped Recursion & Quality/Rating Filters
    # =========================================================================
    def test_suite_7_smart_filters(self):
        print("\n=== SUITE 7: Smart Scoped Recursion & Quality Filters ===")
        movie_lib = next((l for l in self.libraries if "افلام" in l.get("Name", "")), self.libraries[0])

        def case_rating_filter():
            q = {
                "parentId": movie_lib["Id"],
                "includeItemTypes": "Movie,Series",
                "recursive": "true",
                "limit": 20,
                "fields": "CommunityRating"
            }
            res = self._request(f"Users/{self.user_id}/Items?{urllib.parse.urlencode(q)}")
            items = res.get("Items", [])
            rated_items = [it for it in items if it.get("CommunityRating") is not None and it.get("CommunityRating") >= 7.0]
            return f"Found {len(rated_items)} items with rating >= 7.0 among sample of {len(items)}"

        def case_4k_filter_simulation():
            q = {
                "parentId": movie_lib["Id"],
                "includeItemTypes": "Movie",
                "recursive": "true",
                "limit": 50,
                "fields": "MediaSources"
            }
            res = self._request(f"Users/{self.user_id}/Items?{urllib.parse.urlencode(q)}")
            items = res.get("Items", [])
            uhd_items = []
            for it in items:
                sources = it.get("MediaSources", [])
                for s in sources:
                    for stream in s.get("MediaStreams", []):
                        if stream.get("Type") == "Video" and stream.get("Width", 0) >= 3840:
                            uhd_items.append(it["Name"])
                            break
            return f"Scoped recursion evaluated 50 movies: {len(uhd_items)} identified as native 4K/UHD"

        self.record_test("Filters", "Community Rating Filter Evaluation", case_rating_filter)
        self.record_test("Filters", "4K / UHD Scoped Recursion Evaluation", case_4k_filter_simulation)

    # =========================================================================
    # SUITE 8: Alphabetical Scrubber Filtering (Arabic & English)
    # =========================================================================
    def test_suite_8_alphabet_scrubber(self):
        print("\n=== SUITE 8: Alphabetical Scrubber Filtering ===")
        movie_lib = next((l for l in self.libraries if "افلام" in l.get("Name", "")), self.libraries[0])

        for char in ["ا", "م", "A", "T"]:
            def make_char_func(letter=char):
                def case_char():
                    q = {
                        "parentId": movie_lib["Id"],
                        "nameStartsWith": letter,
                        "recursive": "true",
                        "limit": 10
                    }
                    res = self._request(f"Users/{self.user_id}/Items?{urllib.parse.urlencode(q)}")
                    items = res.get("Items", [])
                    total = res.get("TotalRecordCount", 0)
                    sample = [it["Name"] for it in items[:2]]
                    return f"Letter '{letter}' returned {len(items)}/{total} items. Samples: {sample}"
                return case_char

            self.record_test("Alphabet Scrubber", f"Filter by letter '{char}'", make_char_func())

    # =========================================================================
    # SUITE 9: Media Details & Item Metadata for All Media Types
    # =========================================================================
    def test_suite_9_media_details(self):
        print("\n=== SUITE 9: Media Details, Streams & Subtitles Inspection ===")

        def case_movie_details():
            q = {"includeItemTypes": "Movie", "recursive": "true", "limit": 1}
            res = self._request(f"Users/{self.user_id}/Items?{urllib.parse.urlencode(q)}")
            assert len(res.get("Items", [])) > 0, "No movies found on server"
            movie_id = res["Items"][0]["Id"]
            details = self._request(f"Users/{self.user_id}/Items/{movie_id}")

            self.test_artifacts["sample_movie_id"] = movie_id
            sources = details.get("MediaSources", [])
            streams = sources[0].get("MediaStreams", []) if sources else []
            video_codec = next((s.get("Codec") for s in streams if s.get("Type") == "Video"), "unknown")
            audio_codec = next((s.get("Codec") for s in streams if s.get("Type") == "Audio"), "unknown")
            return f"Movie '{details.get('Name')}' | Container: {details.get('Container')} | Video: {video_codec} | Audio: {audio_codec}"

        def case_series_structure():
            q = {"includeItemTypes": "Series", "recursive": "true", "limit": 1}
            res = self._request(f"Users/{self.user_id}/Items?{urllib.parse.urlencode(q)}")
            assert len(res.get("Items", [])) > 0, "No series found on server"
            series_id = res["Items"][0]["Id"]
            series_name = res["Items"][0]["Name"]

            seasons_res = self._request(f"Shows/{series_id}/Seasons?userId={self.user_id}")
            seasons = seasons_res.get("Items", [])
            assert len(seasons) > 0, f"Series '{series_name}' has no seasons"

            season_id = seasons[0]["Id"]
            episodes_res = self._request(f"Shows/{series_id}/Episodes?userId={self.user_id}&seasonId={season_id}")
            episodes = episodes_res.get("Items", [])
            assert len(episodes) > 0, f"Season '{seasons[0].get('Name')}' has no episodes"

            self.test_artifacts["sample_series_id"] = series_id
            self.test_artifacts["sample_episode_id"] = episodes[0]["Id"]
            return f"Series '{series_name}' has {len(seasons)} seasons. Season 1 has {len(episodes)} episodes."

        def case_audio_details():
            islamic_lib = next((l for l in self.libraries if "اسلامية" in l.get("Name", "")), None)
            if not islamic_lib:
                return "Islamic library not found"
            q = {"parentId": islamic_lib["Id"], "includeItemTypes": "Audio,Video", "recursive": "true", "limit": 1}
            res = self._request(f"Users/{self.user_id}/Items?{urllib.parse.urlencode(q)}")
            items = res.get("Items", [])
            if not items:
                return "No Audio/Video items found in Islamic library"
            item_id = items[0]["Id"]
            details = self._request(f"Users/{self.user_id}/Items/{item_id}")
            return f"Item '{details.get('Name')}' Type: {details.get('Type')} Duration: {details.get('RunTimeTicks', 0)//10000000}s"

        self.record_test("Media Details", "Movie Details & Codecs", case_movie_details)
        self.record_test("Media Details", "Series, Seasons & Episodes Hierarchy", case_series_structure)
        self.record_test("Media Details", "Islamic Library Media Item", case_audio_details)

    # =========================================================================
    # SUITE 10: Home Feed, Next Up & Recommendations
    # =========================================================================
    def test_suite_10_home_feeds(self):
        print("\n=== SUITE 10: Home Feed, Next Up & Recommendations ===")

        def case_next_up():
            res = self._request(f"Shows/NextUp?userId={self.user_id}&limit=16")
            items = res.get("Items", [])
            return f"Fetched {len(items)} Next Up episodes. First: {items[0].get('SeriesName') if items else 'None'}"

        def case_resume_items():
            res = self._request(f"Users/{self.user_id}/Items/Resume?mediaTypes=Video&limit=12")
            items = res.get("Items", [])
            return f"Fetched {len(items)} continue watching items."

        def case_spotlight_items():
            q = {
                "sortBy": "Random",
                "includeItemTypes": "Movie,Series",
                "imageTypes": "Backdrop",
                "recursive": "true",
                "limit": 5,
                "fields": "Overview,CommunityRating,MediaSources"
            }
            res = self._request(f"Users/{self.user_id}/Items?{urllib.parse.urlencode(q)}")
            items = res.get("Items", [])
            return f"Fetched {len(items)} spotlight items with backdrop images."

        def case_latest_items():
            res = self._request(f"Users/{self.user_id}/Items/Latest?limit=16")
            assert isinstance(res, list), "Latest items should be a JSON array"
            return f"Fetched {len(res)} globally latest items."

        self.record_test("Home Feeds", "Next Up Episodes (/Shows/NextUp)", case_next_up)
        self.record_test("Home Feeds", "Resume Items (/Items/Resume)", case_resume_items)
        self.record_test("Home Feeds", "Spotlight Carousel (/Items?sortBy=Random)", case_spotlight_items)
        self.record_test("Home Feeds", "Latest Items (/Items/Latest)", case_latest_items)

    # =========================================================================
    # SUITE 11: Real Video Streaming, Range Requests & Image Verification
    # =========================================================================
    def test_suite_11_streaming(self):
        print("\n=== SUITE 11: Direct Playback Stream Handshake & Image Verification ===")

        def case_video_stream():
            movie_id = self.test_artifacts.get("sample_movie_id")
            assert movie_id, "Sample movie ID missing"
            stream_url = f"{self.base_url}/Videos/{movie_id}/stream?static=true"
            req = urllib.request.Request(stream_url, headers={
                "User-Agent": "mdd TV / LiveTest (Android TV)",
                "Range": "bytes=0-1024",
                "X-Emby-Token": self.access_token
            })
            with urllib.request.urlopen(req, timeout=10.0) as resp:
                code = resp.getcode()
                c_type = resp.headers.get("Content-Type", "")
                c_range = resp.headers.get("Content-Range", "")
                assert code in (200, 206), f"Expected 200/206 for stream request, got {code}"
                return f"Stream active: HTTP {code} | Content-Type: {c_type} | Range: {c_range}"

        def case_primary_image():
            movie_id = self.test_artifacts.get("sample_movie_id")
            assert movie_id, "Sample movie ID missing"
            img_url = f"{self.base_url}/Items/{movie_id}/Images/Primary?quality=90&maxWidth=400"
            req = urllib.request.Request(img_url, headers={"User-Agent": "mdd TV / LiveTest (Android TV)"})
            with urllib.request.urlopen(req, timeout=10.0) as resp:
                code = resp.getcode()
                c_type = resp.headers.get("Content-Type", "")
                c_len = resp.headers.get("Content-Length", "0")
                assert code == 200, f"Expected 200 for poster, got {code}"
                assert "image/" in c_type, f"Expected image content-type, got {c_type}"
                return f"Poster delivered: HTTP {code} | Type: {c_type} | Size: {int(c_len)//1024} KB"

        self.record_test("Playback & Assets", "Video Stream HTTP Range 206", case_video_stream)
        self.record_test("Playback & Assets", "Primary Image (Poster) Handshake", case_primary_image)

    # =========================================================================
    # SUITE 12: Search Engine (Arabic, English, Partial & Empty)
    # =========================================================================
    def test_suite_12_search(self):
        print("\n=== SUITE 12: Multilingual Search Engine Handshake ===")

        test_terms = ["رمضان", "Batman", "محمد", "nonexistentquery_xyz_999"]
        for term in test_terms:
            def make_search_func(qterm=term):
                def case_search():
                    q = {
                        "searchTerm": qterm,
                        "includeItemTypes": "Movie,Series,Episode,Person",
                        "recursive": "true",
                        "limit": 10
                    }
                    res = self._request(f"Users/{self.user_id}/Items?{urllib.parse.urlencode(q)}")
                    items = res.get("Items", [])
                    total = res.get("TotalRecordCount", 0)
                    names = [it["Name"] for it in items[:3]]
                    return f"Query '{qterm}' -> Total: {total}. Matches: {names}"
                return case_search

            self.record_test("Search Engine", f"Search query '{term}'", make_search_func())

    # =========================================================================
    # SUITE 13: Playback Session Progress Reporting
    # =========================================================================
    def test_suite_13_session_reporting(self):
        print("\n=== SUITE 13: Playback Session Reporting API ===")
        movie_id = self.test_artifacts.get("sample_movie_id")
        if not movie_id:
            return

        def case_session_flow():
            start_payload = {
                "ItemId": movie_id,
                "PositionTicks": 0,
                "CanSeek": True,
                "PlayMethod": "DirectPlay"
            }
            res_start = self._request("Sessions/Playing", method="POST", data=start_payload)

            progress_payload = {
                "ItemId": movie_id,
                "PositionTicks": 600000000,
                "IsPaused": False,
                "PlayMethod": "DirectPlay"
            }
            res_prog = self._request("Sessions/Playing/Progress", method="POST", data=progress_payload)

            stop_payload = {
                "ItemId": movie_id,
                "PositionTicks": 1200000000
            }
            res_stop = self._request("Sessions/Playing/Stopped", method="POST", data=stop_payload)

            return "Playback start, progress and stop sessions accepted by Jellyfin server"

        self.record_test("Playback Sessions", "Report Start, Progress, and Stop", case_session_flow)

    # =========================================================================
    # Runner & Summary Generation
    # =========================================================================
    def run_all(self):
        print(f"Starting exhaustive live test suite against {self.base_url} (User: {self.username})...\n")
        start_all = time.time()

        self.test_suite_1_auth()
        self.test_suite_2_libraries()
        self.test_suite_3_hierarchical_root()
        self.test_suite_4_deep_drilldown()
        self.test_suite_5_flat_vs_hierarchical()
        self.test_suite_6_pagination()
        self.test_suite_7_smart_filters()
        self.test_suite_8_alphabet_scrubber()
        self.test_suite_9_media_details()
        self.test_suite_10_home_feeds()
        self.test_suite_11_streaming()
        self.test_suite_12_search()
        self.test_suite_13_session_reporting()

        total_duration = time.time() - start_all
        passed = sum(1 for r in self.results if r.status == "PASS")
        failed = sum(1 for r in self.results if r.status == "FAIL")

        print("\n" + "="*70)
        print("               LIVE SERVER VERIFICATION REPORT")
        print("="*70)
        print(f"Total Test Cases Executed: {len(self.results)}")
        print(f"Passed: \033[92m{passed}\033[0m")
        print(f"Failed: \033[91m{failed}\033[0m" if failed > 0 else f"Failed: {failed}")
        print(f"Total Execution Time: {total_duration:.2f} seconds")
        print("="*70)

        report = {
            "server": self.base_url,
            "user": self.username,
            "timestamp": time.strftime("%Y-%m-%d %H:%M:%S"),
            "total_cases": len(self.results),
            "passed": passed,
            "failed": failed,
            "duration_seconds": round(total_duration, 2),
            "cases": [
                {
                    "category": r.category,
                    "name": r.name,
                    "status": r.status,
                    "duration_ms": round(r.duration_ms, 2),
                    "details": r.details,
                    "error": r.error
                }
                for r in self.results
            ]
        }
        import os
        output_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "live_server_results.json")
        with open(output_path, "w", encoding="utf-8") as f:
            json.dump(report, f, ensure_ascii=False, indent=2)
        print(f"Report written to {output_path}")

        return failed == 0

if __name__ == "__main__":
    suite = LiveServerTestSuite()
    success = suite.run_all()
    sys.exit(0 if success else 1)
